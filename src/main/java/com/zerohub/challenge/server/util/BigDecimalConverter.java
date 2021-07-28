package com.zerohub.challenge.server.util;

import com.zerohub.challenge.server.exception.ConvertException;

import java.math.BigDecimal;
import java.text.MessageFormat;

public final class BigDecimalConverter {

    public static BigDecimal toDecimal(String s) {
        BigDecimal conv;
        try {
            conv = new BigDecimal(s.toCharArray());
        } catch (NumberFormatException ex) {
            throw new ConvertException(MessageFormat.format("Incorrect number format: `{0}`", s), ex);
        }
        return conv;
    }

    public static String toString(BigDecimal d) {
        return d.toPlainString();
    }

}
