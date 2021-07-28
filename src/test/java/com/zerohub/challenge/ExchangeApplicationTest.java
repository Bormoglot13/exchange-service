package com.zerohub.challenge;

import com.zerohub.challenge.client.config.GRPCClientConfig;
import com.zerohub.challenge.client.config.ValidatorConfig;
import com.zerohub.challenge.client.service.ExchangeClientService;
import com.zerohub.challenge.client.util.GRPCClientUtil;
import com.zerohub.challenge.client.validator.ConvertRequestDTOValidator;
import com.zerohub.challenge.proto.ConvertRequest;
import com.zerohub.challenge.proto.ConvertResponse;
import com.zerohub.challenge.proto.PublishRequest;
import com.zerohub.challenge.server.config.GRPCServerConfig;
import com.zerohub.challenge.server.repository.CurrencyRateRepository;
import com.zerohub.challenge.server.repository.impl.CurrencyRateRepositoryImpl;
import com.zerohub.challenge.server.service.ExchangeService;
import com.zerohub.challenge.server.util.BigDecimalConverter;
import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.protobuf.ProtoUtils;
import lombok.extern.slf4j.Slf4j;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ContextConfiguration(classes = {
        GRPCServerConfig.class,
        GRPCClientConfig.class,
        GRPCClientUtil.class,
        ValidatorConfig.class,
        ConvertRequestDTOValidator.class,
        ExchangeClientService.class,
        CurrencyRateRepositoryImpl.class,
        ConvertResponse.class,
        ExchangeService.class
})
@ExtendWith(SpringExtension.class)
@DirtiesContext()
@Slf4j
public class ExchangeApplicationTest extends AbstractTest {
  private static final String BTC = "BTC";
  private static final String EUR = "EUR";
  private static final String USD = "USD";
  private static final String UAH = "UAH";
  private static final String RUB = "RUB";
  private static final String LTC = "LTC";
  private static final String AUD = "AUD";
  private static final String CHF = "CHF";

  private ExchangeClientService clientService;

  private ExchangeService serverService;

  private CurrencyRateRepository repo;

  @Autowired
  public ExchangeApplicationTest(ExchangeClientService clientService, ExchangeService serverService, CurrencyRateRepository repo) {
    this.clientService = clientService;
    this.serverService = serverService;
    this.repo = repo;
  }

  @BeforeEach
  public void setup() {
    var rates = List.of(
      toPublishRequest(new String[]{BTC, EUR, "50000.0000"}),
      toPublishRequest(new String[]{EUR, USD, "1.2000"}),
      toPublishRequest(new String[]{EUR, AUD, "1.5000"}),
      toPublishRequest(new String[]{USD, RUB, "80.0000"}),
      toPublishRequest(new String[]{UAH, RUB, "4.0000"}),
      toPublishRequest(new String[]{LTC, BTC, "0.0400"}),
      toPublishRequest(new String[]{LTC, USD, "2320.0000"}),
      toPublishRequest(new String[]{BTC, USD, "60000.0000"})
    );
    for (var rate : rates) {
      // publish new data to com.zerohub.challenge.client.service
      clientService.publish(rate);
    }
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("testDataPriceEquals")
  void ConvertTest(String ignore, ConvertRequest request, BigDecimal expectedPrice) {

    // Request com.zerohub.challenge.client.service and get price
    ConvertResponse convertResponse = clientService.getPrice(request);
    String price = convertResponse.getPrice();
    BigDecimal actual = BigDecimalConverter.toDecimal(price);

    assertEquals(actual, expectedPrice);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("testDataMatcher")
  void ConvertTest(String ignore, ConvertRequest request, @NotNull Matcher<List<String>> expected) {

    // Request com.zerohub.challenge.client.service and get price
    ConvertResponse convertResponse = clientService.getPrice(request);
    String price = convertResponse.getPrice();

    // check conversion BigDecimal->String
    List<BigDecimal> actualTemp = List.of(BigDecimalConverter.toDecimal(price));
    List<String> actual = actualTemp.stream().map(BigDecimal::toPlainString).collect(Collectors.toList());

    assertThat(actual, expected);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("testDataException")
  void ConvertTest(String ignore, ConvertRequest request, StatusRuntimeException expected) {

    // Request com.zerohub.challenge.client.service and get price
    Assert.assertThrows(expected.getMessage(), expected.getClass(), () -> clientService.getPrice(request));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("testDataRateEquals")
  void ConvertTest(String ignore, ConvertRequest request, BigDecimal expectedRate, String dummyMarker) {

    // Request com.zerohub.challenge.client.service and get price
    var rate = serverService.getPriceBestRateWithOneHop(request);
    BigDecimal actual = BigDecimalConverter.toDecimal(rate);

    assertEquals(actual, expectedRate);
  }

  private static Stream<Arguments> testDataPriceEquals() {

    return Stream.of(
            Arguments.of("Same currency", toConvertRequest(new String[]{BTC, BTC, "0.9997"}), "0.9997"),
            Arguments.of("Simple conversion", toConvertRequest(new String[]{EUR, BTC, "50000.0000"}), "1.0000"),
            Arguments.of("Reversed conversion", toConvertRequest(new String[]{BTC, EUR, "1.0000"}), "50000.0000"),
            Arguments.of("Convert with one hop", toConvertRequest(new String[]{BTC, AUD, "1.0000"}), "75000.0000"),
            Arguments.of("Reversed conversion with two hops", toConvertRequest(new String[]{RUB, EUR, "96.0000"}), "1.0000"),
            Arguments.of("Convert small amount", toConvertRequest(new String[]{RUB, EUR, "12.3200"}), "0.1284")
// TODO  additional test case
//            , Arguments.of("Calculate best price(make through traverse graph with estimation of node values)",
//                    toConvertRequest(new String[]{BTC, USD, "2.0000"}), "116000.0000")

    );
  }

  private static Stream<Arguments> testDataMatcher() {

    return Stream.of(
            // BTC->RUB may be computed 3 path:
            // BTC->EUR->USD->RUB 480000
            // BTC->LTC->USD->RUB 725
            // BTC->USD->RUB      480000
            Arguments.of("Convert with two hops", toConvertRequest(new String[]{BTC, RUB, "1.0000"}),
                    Matchers.anyOf(Matchers.contains("4800000.0000"), Matchers.contains("725.0000")))
    );
  }

  private static Stream<Arguments> testDataException() {

    Metadata.Key<ConvertResponse> CONVERT_RESPONSE_KEY =
            ProtoUtils.keyForProto(ConvertResponse.getDefaultInstance());
    Metadata metadata = new Metadata();
    metadata.put(CONVERT_RESPONSE_KEY, toConvertResponse(new String[]{""}));
    return Stream.of(
            Arguments.of("No conversion found", toConvertRequest(new String[]{CHF, EUR, "13"}),
                    Status.NOT_FOUND.withDescription("No conversion found")
                            .asRuntimeException(metadata))

    );
  }


  private static Stream<Arguments> testDataRateEquals() {

    return Stream.of(
          Arguments.of("Calculate best rate(no more than one hop)", toConvertRequest(new String[]{BTC, USD, "1.0000"}),
                  "58000.0000", "dummymarker")

    );
  }

  private static PublishRequest toPublishRequest(String[] args) {
    return PublishRequest
      .newBuilder()
      .setBaseCurrency(args[0])
      .setQuoteCurrency(args[1])
      .setPrice(args[2])
      .build();
  }

  private static ConvertRequest toConvertRequest(String[] args) {
    return ConvertRequest
      .newBuilder()
      .setFromCurrency(args[0])
      .setToCurrency(args[1])
      .setFromAmount(args[2])
      .build();
  }

  private static ConvertResponse toConvertResponse(String[] args) {
    return ConvertResponse
      .newBuilder()
      .setPrice(args[0])
      .build();
  }

}
