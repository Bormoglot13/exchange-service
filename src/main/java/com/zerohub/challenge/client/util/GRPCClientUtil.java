package com.zerohub.challenge.client.util;

import com.google.common.base.Verify;
import com.zerohub.challenge.client.config.properties.GRPCClientProp;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.http.converter.protobuf.ProtobufHttpMessageConverter;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static org.springframework.beans.factory.config.ConfigurableBeanFactory.SCOPE_PROTOTYPE;

@Component
@Scope( scopeName = SCOPE_PROTOTYPE, proxyMode = ScopedProxyMode.TARGET_CLASS )
public class GRPCClientUtil {

    @Autowired
    private GRPCClientProp client;

    private ManagedChannel channel;

    @Bean
    @Lazy
    public ManagedChannel gRPCManagedChannel() {
        channel = ManagedChannelBuilder.forAddress("localhost", client.getPort())
                .usePlaintext()
                .build();
        return channel;
    }

    @Bean
    public ProtobufHttpMessageConverter protobufHttpMessageConverter() {
        return new ProtobufHttpMessageConverter();
    }

    public ManagedChannel getChannel() {
        if (Objects.isNull(channel) || channel.isShutdown()) {
            gRPCManagedChannel();
        }
        return channel;
    }

    public void shutdownManagedChannel() {
        shutdownManagedChannel(channel);
    }

    private void shutdownManagedChannel(ManagedChannel channel) {
        if (Objects.isNull(channel) || channel.isShutdown()) {
            return;
        }
        long timeoutMs = client.getChannelShutdownTimeoutMs();
        channel.shutdown();
        try {
            channel.shutdown().awaitTermination(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            // Allow thread to exit.
        } finally {
            channel.shutdownNow();
        }
        Verify.verify(channel.isShutdown());
    }

}
