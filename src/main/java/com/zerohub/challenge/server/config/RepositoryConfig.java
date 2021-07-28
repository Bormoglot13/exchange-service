package com.zerohub.challenge.server.config;

import com.zerohub.challenge.server.repository.CurrencyRateRepository;
import com.zerohub.challenge.server.repository.impl.CurrencyRateRepositoryImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RepositoryConfig {

    @Bean
    CurrencyRateRepository publishReqRepository() {
        return new CurrencyRateRepositoryImpl();
    }

}
