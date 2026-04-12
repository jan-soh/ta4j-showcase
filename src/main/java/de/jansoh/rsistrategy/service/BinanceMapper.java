package de.jansoh.rsistrategy.service;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.StringJoiner;

public class BinanceMapper {

    private static int precision = 4;

    private static boolean isDecimal(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        return str.contains(".");
    }

    public static String toFormData(Object pojo) {
        StringJoiner joiner = new StringJoiner("&");
        Field[] fields = pojo.getClass().getDeclaredFields();

        for (Field field : fields) {
            field.setAccessible(true);
            try {
                Object value = field.get(pojo);
                if (value != null) {
                    String name = field.getName();
                    String valueString;
                    if (value instanceof BigDecimal bigDecimal) {
                        valueString = bigDecimal.setScale(precision, RoundingMode.HALF_UP).toPlainString();
                    } else if (value instanceof String s && isDecimal(s)) {
                        try {
                            valueString = new BigDecimal(s).setScale(precision, RoundingMode.HALF_UP).toPlainString();
                        } catch (NumberFormatException e) {
                            valueString = s;
                        }
                    } else {
                        valueString = value.toString();
                    }
                    String encodedValue = URLEncoder.encode(valueString, StandardCharsets.UTF_8);
                    joiner.add(name + "=" + encodedValue);
                }
            } catch (IllegalAccessException e) {
                // Ignore or log
            }
        }
        return joiner.toString();
    }
}
