package de.jansoh.rsistrategy.service;

import java.lang.reflect.Field;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

public class BinanceMapper {

    public static String toFormData(Object pojo) {
        StringJoiner joiner = new StringJoiner("&");
        Field[] fields = pojo.getClass().getDeclaredFields();

        for (Field field : fields) {
            field.setAccessible(true);
            try {
                Object value = field.get(pojo);
                if (value != null) {
                    String name = field.getName();
                    String encodedValue = URLEncoder.encode(value.toString(), StandardCharsets.UTF_8);
                    joiner.add(name + "=" + encodedValue);
                }
            } catch (IllegalAccessException e) {
                // Ignore or log
            }
        }
        return joiner.toString();
    }
}
