package de.jansoh.rsistrategy.service.broker.binance;

import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.StringJoiner;

/**
 * A utility class that provides mapping functionality for converting objects into
 * URL-encoded form data. This class is specifically tailored for use with Binance API.
 */
@Slf4j
public class BinanceMapper {

    /**
     * Converts the fields of a given object into a URL-encoded form data string.
     * This method iterates over all declared fields of the object's class, retrieves their values,
     * and formats them as key-value pairs separated by "&". The field values are URL-encoded
     * to ensure proper formatting for HTTP requests.
     *
     * @param pojo the object whose fields are to be converted to form data; should be a POJO
     *             with declared fields representing the key-value pairs.
     * @return a URL-encoded string representing the form data, where each key-value pair is
     * separated by "&". Returns an empty string if the object has no fields or
     * all fields are null.
     */
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
                        valueString = bigDecimal.toPlainString();
                    } else if (value instanceof String s && isDecimal(s)) {
                        try {
                            valueString = new BigDecimal(s).toPlainString();
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
                log.error("----- BinanceMapper ----- Error accessing field: {}", field.getName(), e);
            }
        }
        return joiner.toString();
    }

    private static boolean isDecimal(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        return str.contains(".");
    }
}
