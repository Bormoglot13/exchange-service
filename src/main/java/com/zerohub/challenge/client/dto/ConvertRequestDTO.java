package com.zerohub.challenge.client.dto;

import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@SuperBuilder
@Data
@NoArgsConstructor
@FieldDefaults( level = AccessLevel.PRIVATE )
public class ConvertRequestDTO {

    String fromCurrency;

    BigDecimal fromAmount;

    String  toCurrency;

}
