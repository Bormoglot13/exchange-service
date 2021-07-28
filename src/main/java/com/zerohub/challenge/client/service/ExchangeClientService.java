package com.zerohub.challenge.client.service;

import com.zerohub.challenge.client.dto.ConvertRequestDTO;
import com.zerohub.challenge.client.dto.PublishRequestDTO;
import com.zerohub.challenge.client.util.GRPCClientUtil;
import com.zerohub.challenge.client.validator.ConvertRequestDTOValidator;
import com.zerohub.challenge.proto.ConvertRequest;
import com.zerohub.challenge.proto.ConvertResponse;
import com.zerohub.challenge.proto.PublishRequest;
import com.zerohub.challenge.proto.RatesServiceGrpc;
import io.grpc.ManagedChannel;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.BindingResult;

@Service
@Slf4j
public class ExchangeClientService {

    @Autowired
    private GRPCClientUtil grpcClientUtil;

    @Autowired
    private ConvertRequestDTOValidator convertRequestDTOValidator;

    @SneakyThrows
    public void publish(PublishRequest req) {
        ManagedChannel channel = grpcClientUtil.getChannel();
        RatesServiceGrpc.RatesServiceBlockingStub stub = RatesServiceGrpc.newBlockingStub(channel);
        stub.publish(req);
        grpcClientUtil.shutdownManagedChannel();
    }

    @SneakyThrows
    public ConvertResponse getPrice(ConvertRequest req) {
        ManagedChannel channel = grpcClientUtil.getChannel();
        RatesServiceGrpc.RatesServiceBlockingStub stub = RatesServiceGrpc.newBlockingStub(channel);
        ConvertResponse resp = stub.convert(req);
        grpcClientUtil.shutdownManagedChannel();
        return resp;
    }

    public void validate(ConvertRequestDTO dto, BindingResult bindingResult) {
        convertRequestDTOValidator.validate(dto, bindingResult);
    }

    public void validate(PublishRequestDTO dto, BindingResult bindingResult) {
        // TODO not implemented
    }

}
