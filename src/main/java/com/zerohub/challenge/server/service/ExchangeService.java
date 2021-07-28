package com.zerohub.challenge.server.service;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.protobuf.Empty;
import com.zerohub.challenge.proto.ConvertRequest;
import com.zerohub.challenge.proto.ConvertResponse;
import com.zerohub.challenge.proto.PublishRequest;
import com.zerohub.challenge.proto.RatesServiceGrpc;
import com.zerohub.challenge.server.exception.ConversionNotFoundException;
import com.zerohub.challenge.server.repository.CurrencyRateRepository;
import com.zerohub.challenge.server.util.BigDecimalConverter;
import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.protobuf.ProtoUtils;
import io.grpc.stub.StreamObserver;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.IntStream;


@Slf4j
@GrpcService
@NoArgsConstructor
@Service
public final class ExchangeService extends RatesServiceGrpc.RatesServiceImplBase {


    private CurrencyRateRepository repo;

    @Getter
    private ConvertResponse convertResponse;

    @Autowired
    public ExchangeService(CurrencyRateRepository repo) {
        this.repo = repo;
        this.convertResponse = ConvertResponse.newBuilder().build();
    }

    @Override
    public void publish(PublishRequest req, StreamObserver<com.google.protobuf.Empty> responseObserver) {
        repo.save(req);
        responseObserver.onNext(Empty.newBuilder().build());
        responseObserver.onCompleted();

    }

    @Override
    public void convert(ConvertRequest req, StreamObserver<ConvertResponse> responseObserver){
        try {
            getPriceWithSameCurrency(req);
            getPriceWithSimpleAndReverseConversion(req);
            getPriceWithHops(req);
            // some others computing of conversion paths and etc.

            responseObserver.onNext(this.convertResponse);
            responseObserver.onCompleted();
        } catch (ConversionNotFoundException ex) {
            log.debug("ConvertRequest: {}, {}", req, ex);
            Metadata.Key<ConvertResponse> CONVERT_RESPONSE_KEY =
                    ProtoUtils.keyForProto(ConvertResponse.getDefaultInstance());
            Metadata metadata = new Metadata();
            metadata.put(CONVERT_RESPONSE_KEY, this.convertResponse);
            responseObserver.onError(
                    Status.NOT_FOUND.withDescription(ex.getMessage())
                            .asRuntimeException(metadata));
        } finally {
            // set convertResponse.price to empty for next requests
            this.convertResponse = ConvertResponse.newBuilder().build();
        }
    }

    private ExchangeService getPriceWithSameCurrency(ConvertRequest req) {
        if (skipConversion("Skip same currency...")) {
            return this;
        }
        if (req.getFromCurrency().equals(req.getToCurrency())) {
            this.convertResponse = toConvertResponse(BigDecimalConverter.toDecimal(req.getFromAmount()));
            log.debug("Same Currency: {}", req);
        }
        return this;
    }

    private ExchangeService getPriceWithSimpleAndReverseConversion(ConvertRequest req) {
        if (skipConversion("Skip simple and reverse conversion...")) {
            return this;
        }
        var currencyValue = repo.findPriceByBaseCurrencyAndQuoteCurrency(req.getFromCurrency(),req.getToCurrency());
        if (currencyValue.isPresent()) {
            BigDecimal amount = BigDecimalConverter.toDecimal(req.getFromAmount());
            BigDecimal rate = BigDecimalConverter.toDecimal(currencyValue.get().getPrice());
            BigDecimal price = amount.multiply(rate).setScale(4, RoundingMode.UP);
            this.convertResponse = toConvertResponse(price);
        }
        return this;
    }

    public String getPriceBestRateWithOneHop(ConvertRequest req){
        log.debug("Estimate best conversion with one hop...");

        String nodeU = req.getFromCurrency();
        String nodeV = req.getToCurrency();
        List<List<String>> listConversions = new ArrayList<>();
        Map<String, BigDecimal> enrichmentConversions = new HashMap<>();

        // create graph and will build all paths
        var graph = repo.createUndirectedGraph();

        // same currency
        if (nodeU.equals(nodeV)) {
            listConversions.add(ImmutableList.of(nodeU));
        }
        Set<String> successors = ImmutableSet.copyOf(graph.successors(nodeU));

        // exists edge between nodes
        if (successors.contains(nodeV)) {
            listConversions.add(ImmutableList.of(nodeU, nodeV));
        }

        Set<String> predecessors = ImmutableSet.copyOf(graph.predecessors(nodeV));
        Set<String> intersection = new HashSet<>(successors);
        intersection.retainAll(predecessors);

        // exists relationship  with 1 hop
        intersection.forEach(e -> listConversions.add(ImmutableList.of(nodeU, e, nodeV)));

        // filter for best rate
        if (!listConversions.isEmpty()) {
            for (var conv: listConversions) {
                var ref = new Object() {
                    BigDecimal rate = null;
                };
                StringBuffer sb = new StringBuffer();
                IntStream.range(0, conv.size() - 1 )
                        .forEach( i -> {
                            var currencyValue =
                                    repo.findPriceByBaseCurrencyAndQuoteCurrency(conv.get(i), conv.get(i+1));
                            if (currencyValue.isPresent()) {
                                BigDecimal rate = BigDecimalConverter.toDecimal(currencyValue.get().getPrice());
                                // get conversion rate in cycle of conversions
                                ref.rate = (Objects.isNull(ref.rate))? rate : ref.rate.multiply(rate);
                                if (sb.length() == 0) {
                                    sb.append(conv.get(i))
                                            .append(" -> ")
                                            .append(conv.get(i+1));
                                } else {
                                    sb.append(" -> ")
                                            .append(conv.get(i+1));
                                }

                            }
                        });

                BigDecimal rate = Optional.ofNullable(ref.rate)
                        .orElseThrow(() -> new ConversionNotFoundException("No conversion found"))
                        .setScale(4, RoundingMode.UP);
                enrichmentConversions.put(sb.toString(), rate);
            }
            Optional<Map.Entry<String, BigDecimal>> minEntry = enrichmentConversions.entrySet()
                    .stream()
                    .min(Comparator.comparing(Map.Entry::getValue));
            String bestRate =  BigDecimalConverter.toString(minEntry
                    .orElseThrow( () -> new ConversionNotFoundException("No conversion found"))
                    .getValue());
            log.debug("All conversions: {}", enrichmentConversions);
            log.debug("Estimated best rate: `{}`",  minEntry);

            return bestRate;
        } else {
            throw new ConversionNotFoundException("No conversion found");
        }
    }

    private ExchangeService getPriceWithHops(ConvertRequest req) {
        if (skipConversion("Skip conversion with hops...")) {
            return this;
        }
        var route = repo.findRoute(req.getFromCurrency(), req.getToCurrency());
        if (route.isPresent()) {
            var ref = new Object() {
                BigDecimal rate = null;
            };
            route.get().keySet().forEach(key -> {
                var currencyValue = repo.findPriceByBaseCurrencyAndQuoteCurrency(key, route.get().get(key));
                if (currencyValue.isPresent()) {
                    BigDecimal rate = BigDecimalConverter.toDecimal(currencyValue.get().getPrice());
                    // get conversion rate in cycle of conversions
                    ref.rate = (Objects.isNull(ref.rate))? rate : ref.rate.multiply(rate);
                }
            });
            // get total conversion price with rounding up
            BigDecimal amount = BigDecimalConverter.toDecimal(req.getFromAmount());
            BigDecimal price = Optional.ofNullable(ref.rate)
                    .orElseThrow(() -> new ConversionNotFoundException("No conversion found"))
                    .multiply(amount).setScale(4, RoundingMode.UP);
            this.convertResponse = toConvertResponse(price);
        }
        return this;
    }

    private Boolean skipConversion(String logmessage) {
        if (!this.convertResponse.getPrice().isEmpty()) {
            log.debug(logmessage);
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }

    private ConvertResponse toConvertResponse(BigDecimal price) {
        return ConvertResponse
                .newBuilder()
                .setPrice(BigDecimalConverter.toString(price))
                .build();
    }




}

