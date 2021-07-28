package com.zerohub.challenge.server.repository.impl;

import com.google.common.graph.GraphBuilder;
import com.google.common.graph.MutableGraph;
import com.zerohub.challenge.proto.PublishRequest;
import com.zerohub.challenge.server.exception.ConversionNotFoundException;
import com.zerohub.challenge.server.model.CurrencyPair;
import com.zerohub.challenge.server.model.CurrencyValue;
import com.zerohub.challenge.server.model.graphs.CurrencyGraphs;
import com.zerohub.challenge.server.repository.CurrencyRateRepository;
import com.zerohub.challenge.server.util.BigDecimalConverter;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;

@Slf4j
public class CurrencyRateRepositoryImpl implements CurrencyRateRepository {

    private Map<CurrencyPair, CurrencyValue> publishRates = new ConcurrentHashMap<>();

    @Override
    public Optional<CurrencyValue> findPriceByBaseCurrencyAndQuoteCurrency(String baseCurrency, String quoteCurrency) {
        CurrencyPair key = toCurrencyPair(baseCurrency, quoteCurrency);
        // direct request
        Optional<CurrencyValue>  result = Optional.ofNullable(publishRates.get(key));

        // reverse request
        if (result.isEmpty()) {
            key = toCurrencyPair(quoteCurrency, baseCurrency);
            result = Optional.ofNullable(publishRates.get(key));
            if (result.isPresent()) {
                BigDecimal number = BigDecimalConverter.toDecimal(result.get().getPrice());
                number = (BigDecimal.valueOf(1.0)).divide(number, MathContext.DECIMAL128);
                result = Optional.of(
                        toCurrencyValue(BigDecimalConverter.toString(number))
                );
            }
        }

        return result;
    }

    @Override
    public Optional<Map<CurrencyPair, CurrencyValue>> findAll() {
        return Optional.of(publishRates);
    }

    @Override
    public Optional<Map<String, String>> findRoute(String baseCurrency, String quoteCurrency) {
//        int hopStep = 0;
        Map<String, String> route = new TreeMap<>();

        var graph = createUndirectedGraph();
        // TODO implemented method for best conversion and use him
        //  in last test
        List<String> list;
        try {
            list = CurrencyGraphs.shortestPath(graph, baseCurrency, quoteCurrency);
        } catch(IllegalArgumentException ex) {
            throw new ConversionNotFoundException("No conversion found", ex);
        }
//        // graph.edges().contains(new EndpointPair(baseCurrency,quoteCurrency));
//        List<String> list2 = CurrencyGraphs.shortestPath(graph,baseCurrency, quoteCurrency);
//        graph.predecessors(quoteCurrency).contains(baseCurrency);
//        graph.successors(baseCurrency);
//        graph.edges().stream().map(e->{
//            return graph.incidentEdges(e.nodeV()).equals(EndpointPair.unordered(baseCurrency,quoteCurrency));
//        }).toArray();
//
//        try {
//            Iterable<String> result = Iterables.limit(Traverser.forGraph(graph).breadthFirst(baseCurrency), hopStep + 2);
//            while ( (list.isEmpty() || Objects.isNull(result)) && hopStep < Iterators.size(result.iterator()) ) {
//                if (Objects.nonNull(result) && quoteCurrency.equals(Iterators.getLast(result.iterator()))) {
//                    result.forEach(x -> list.add(x));
//                } else {
//                    hopStep++;
//                    result = Iterables.limit(Traverser.forGraph(graph).breadthFirst(baseCurrency), hopStep + 2);
//                }
//            }
//        } catch (IllegalArgumentException ex) {
//            log.debug("Not find conversion, {}", ex);
//            throw new RuntimeException("No conversion found");
//        }
        IntStream.range(0, list.size() - 1 ).forEach( i -> route.put(list.get(i), list.get(i+1)) );

        return Optional.of(route);
    }

//    @Override
//    public Optional<Map<String, String>> findAllKeys() {
//        return Optional.of(publishRates.keySet()
//                .stream()
//                .collect(Collectors.toMap( (k -> k.getBaseCurrency()), (e -> e.getQuoteCurrency()),
//                        (prev, next) -> next, TreeMap::new )));
//    }

    private MutableGraph<String> createDirectedGraph() {
        MutableGraph<String> graph = GraphBuilder.directed().build();
        return createGraph(graph);
    }

    public MutableGraph<String> createUndirectedGraph() {
        MutableGraph<String> graph = GraphBuilder.undirected().build();
        return createGraph(graph);
    }

    private MutableGraph<String> createGraph(MutableGraph<String> graph) {
        publishRates.keySet().stream()
                .forEach(k -> {
                    graph.putEdge(k.getBaseCurrency(), k.getQuoteCurrency());

//                    String nodeU = k.getBaseCurrency();
//                    String nodeV = k.getQuoteCurrency();
//                    if (graph.nodes().contains(nodeU)) {
//                        if (!graph.nodes().contains(nodeV)) {
//                            graph.addNode(nodeV);
//                            graph.putEdge(nodeU, nodeV);
//                        }
//                    } else {
//                        graph.addNode(nodeU);
//                        if (graph.nodes().contains(nodeV)) {
//                            graph.putEdge(nodeV, nodeU);
//                        } else {
//                            graph.addNode(nodeV);
//                            graph.putEdge(nodeU, nodeV);
//                        }
//                    }
                });

        return graph;
    }


    @Override
    public void save(PublishRequest req) {
        CurrencyPair key = toCurrencyPair(req.getBaseCurrency(),req.getQuoteCurrency());
        CurrencyValue value = toCurrencyValue(req.getPrice());
        publishRates.put(key, value);
    }


    private CurrencyPair toCurrencyPair(String baseCurrency, String quoteCurrency) {
        return CurrencyPair.builder()
                .baseCurrency(baseCurrency)
                .quoteCurrency(quoteCurrency)
                .build();
    }

    private CurrencyValue toCurrencyValue(String price) {
        return CurrencyValue.builder()
                .price(price)
                .build();
    }


}
