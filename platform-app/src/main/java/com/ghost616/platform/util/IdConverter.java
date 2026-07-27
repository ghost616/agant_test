package com.ghost616.platform.util;

import java.util.Collections;
import java.util.List;

public class IdConverter {

    public static Long parse(String str) {
        if (str == null || str.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(str.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid ID format: " + str, e);
        }
    }

    public static String toString(Long value) {
        return value != null ? value.toString() : null;
    }

    public static List<Long> parseList(List<String> strList) {
        if (strList == null) {
            return null;
        }
        return strList.stream()
                .map(IdConverter::parse)
                .toList();
    }

    public static List<String> toStringList(List<Long> valueList) {
        if (valueList == null) {
            return null;
        }
        return valueList.stream()
                .map(IdConverter::toString)
                .toList();
    }
}
