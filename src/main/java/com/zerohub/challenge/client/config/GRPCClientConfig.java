package com.zerohub.challenge.client.config;

import com.zerohub.challenge.client.config.properties.GRPCClientProp;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties
public class GRPCClientConfig {

    @Bean
    @ConfigurationProperties(prefix = "grpc.client")
    public GRPCClientProp client() {
        return new GRPCClientProp();
    }

}
