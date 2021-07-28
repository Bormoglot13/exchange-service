package com.zerohub.challenge.server.config;

import com.zerohub.challenge.server.config.properties.GRPCServerProp;
import com.zerohub.challenge.server.service.ExchangeService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.io.IOException;

@Configuration
@EnableConfigurationProperties
@Slf4j
public class GRPCServerConfig {

    @Autowired
    private ExchangeService service;

    @Bean
    @ConfigurationProperties(prefix = "grpc.server")
    public GRPCServerProp server() {
        return new GRPCServerProp();
    }

    // TODO move in TestConfiguration
    // for work application without tests commented this
    @PostConstruct
    public Server startServer() throws IOException, InterruptedException {
        // Create a new server to listen on port
        Server server = ServerBuilder.forPort(this.server().getPort())
                .addService(service)
                .build();

        // Start the server
        server.start();

        // Server threads are running in the background.
        log.info("Server started");
        // Don't exit the main thread. Wait until server is terminated.
        // server.awaitTermination();
        return server;
    }

}
