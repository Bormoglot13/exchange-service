package com.zerohub.challenge.server.repository;

import com.google.common.graph.MutableGraph;
import com.zerohub.challenge.proto.PublishRequest;
import com.zerohub.challenge.server.model.CurrencyPair;
import com.zerohub.challenge.server.model.CurrencyValue;

import java.util.Map;
import java.util.Optional;

public interface CurrencyRateRepository {

    Optional<CurrencyValue> findPriceByBaseCurrencyAndQuoteCurrency(String baseCurrency, String quoteCurrency);
    Optional<Map<CurrencyPair, CurrencyValue>> findAll();
    Optional<Map<String, String>> findRoute(String baseCurrency, String quoteCurrency);
    void save(PublishRequest publishRequest);
    MutableGraph<String> createUndirectedGraph();
}
