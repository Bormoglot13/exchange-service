package com.zerohub.challenge.server.model;

import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Data
@NoArgsConstructor
@FieldDefaults( level = AccessLevel.PRIVATE )
public class CurrencyPair {

    String baseCurrency;

    String  quoteCurrency;

}
