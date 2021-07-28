package com.zerohub.challenge.client.controller;

import com.zerohub.challenge.client.dto.ConvertRequestDTO;
import com.zerohub.challenge.client.dto.ConvertResponseDTO;
import com.zerohub.challenge.client.dto.CurrencyValueDTO;
import com.zerohub.challenge.client.dto.PublishRequestDTO;
import com.zerohub.challenge.client.service.ExchangeClientService;
import com.zerohub.challenge.proto.ConvertRequest;
import com.zerohub.challenge.proto.PublishRequest;
import com.zerohub.challenge.server.mapper.ExchangeMapper;
import com.zerohub.challenge.server.model.CurrencyPair;
import com.zerohub.challenge.server.repository.CurrencyRateRepository;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@RestController
@Validated
@RequestMapping( consumes = { MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_OCTET_STREAM_VALUE })
public class ExchangeRestController {

    private ExchangeClientService service;

    private CurrencyRateRepository repo;

    @Autowired
    public ExchangeRestController(ExchangeClientService service, CurrencyRateRepository repo) {
        this.service = service;
        this.repo = repo;

    }

    @GetMapping( value = "/rate" )
    @ResponseBody
    public ResponseEntity<Optional<Map<CurrencyPair, CurrencyValueDTO>>> rate() {
        Map<CurrencyPair, CurrencyValueDTO> dto = repo.findAll()
                .orElseThrow(() -> new RuntimeException("No rates found"))
                .entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        e -> ExchangeMapper.currencyValueFromProtoToDto(e.getValue())));
        return ResponseEntity.ok(Optional.of(dto));
    }

    @PostMapping( value = "/convert" )
    @ResponseBody
    public ResponseEntity<ConvertResponseDTO> convert(@Valid @RequestBody @NonNull ConvertRequestDTO req,
                                                      final BindingResult bindingResult) {
        service.validate(req, bindingResult);
        // TODO make implementation with mapStruct
        ConvertRequest proto = ExchangeMapper.convertReqFromDtoToProto(req);
        ConvertResponseDTO respDTO = ExchangeMapper.convertRespFromProtoToDto(service.getPrice(proto));
        return ResponseEntity.ok(respDTO);
    }

    @PostMapping( value = "/publish" )
    @ResponseBody
    public ResponseEntity<Void> publish(@Valid @RequestBody @NonNull PublishRequestDTO req,
                                        final BindingResult bindingResult) {
        service.validate(req, bindingResult);
        PublishRequest proto = ExchangeMapper.publishReqFromDtoToProto(req);
        service.publish(proto);
        return ResponseEntity.status( HttpStatus.OK ).build();
    }

}

