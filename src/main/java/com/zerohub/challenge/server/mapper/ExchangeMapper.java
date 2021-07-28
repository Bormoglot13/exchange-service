package com.zerohub.challenge.server.mapper;

import com.zerohub.challenge.client.dto.ConvertRequestDTO;
import com.zerohub.challenge.client.dto.ConvertResponseDTO;
import com.zerohub.challenge.client.dto.CurrencyValueDTO;
import com.zerohub.challenge.client.dto.PublishRequestDTO;
import com.zerohub.challenge.proto.ConvertRequest;
import com.zerohub.challenge.proto.ConvertResponse;
import com.zerohub.challenge.proto.PublishRequest;
import com.zerohub.challenge.server.model.CurrencyValue;
import com.zerohub.challenge.server.util.BigDecimalConverter;
import org.springframework.stereotype.Component;

@Component
public final class ExchangeMapper {

    public static PublishRequest publishReqFromDtoToProto(PublishRequestDTO req) {
        return PublishRequest.newBuilder()
                .setBaseCurrency(req.getBaseCurrency())
                .setQuoteCurrency(req.getQuoteCurrency())
                .setPrice(req.getPrice().toString())
                .build();
    }

    public static CurrencyValueDTO currencyValueFromProtoToDto(CurrencyValue req) {
        return CurrencyValueDTO.builder()
                .price(BigDecimalConverter.toDecimal(req.getPrice()))
                .build();
    }

    public static ConvertRequest convertReqFromDtoToProto(ConvertRequestDTO req) {
        return ConvertRequest
                .newBuilder()
                .setFromCurrency(req.getFromCurrency())
                .setToCurrency(req.getToCurrency())
                .setFromAmount(req.getFromAmount().toString())
                .build();
    }

    public static ConvertResponseDTO convertRespFromProtoToDto(ConvertResponse resp) {
        return ConvertResponseDTO
                .builder()
                .price(BigDecimalConverter.toDecimal(resp.getPrice()))
                .build();
    }

}
