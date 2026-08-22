package com.loyalty.capstone.gateway;

import java.util.LinkedHashMap;
import java.util.Map;

/** Minimal JSON support so the runtime needs no third-party library. */
public final class Json {

    private Json() { }

    public static String escape(String value) {
        StringBuilder out = new StringBuilder();
        for (char c : value.toCharArray()) {
            if (c == '"' || c == '\\') out.append('\\').append(c);
            else if (c == '\n') out.append("\\n");
            else out.append(c);
        }
        return out.toString();
    }

    public static String object(Object... keyThenValue) {
        StringBuilder out = new StringBuilder("{");
        for (int i = 0; i < keyThenValue.length; i += 2) {
            if (i > 0) out.append(',');
            out.append('"').append(escape(String.valueOf(keyThenValue[i]))).append("\":");
            Object value = keyThenValue[i + 1];
            if (value == null) out.append("null");
            else if (value instanceof Number || value instanceof Boolean) out.append(value);
            else out.append('"').append(escape(String.valueOf(value))).append('"');
        }
        return out.append('}').toString();
    }

    /** Parses a flat JSON object of string and number values. */
    public static Map<String, String> parseFlat(String body) {
        Map<String, String> fields = new LinkedHashMap<>();
        if (body == null) return fields;
        String trimmed = body.trim();
        if (trimmed.startsWith("{")) trimmed = trimmed.substring(1);
        if (trimmed.endsWith("}")) trimmed = trimmed.substring(0, trimmed.length() - 1);

        boolean inQuotes = false;
        StringBuilder current = new StringBuilder();
        java.util.List<String> parts = new java.util.ArrayList<>();
        for (char c : trimmed.toCharArray()) {
            if (c == '"') inQuotes = !inQuotes;
            if (c == ',' && !inQuotes) { parts.add(current.toString()); current.setLength(0); }
            else current.append(c);
        }
        if (current.length() > 0) parts.add(current.toString());

        for (String part : parts) {
            int colon = indexOfUnquoted(part);
            if (colon < 0) continue;
            String key = strip(part.substring(0, colon));
            String value = strip(part.substring(colon + 1));
            fields.put(key, value);
        }
        return fields;
    }

    private static int indexOfUnquoted(String part) {
        boolean inQuotes = false;
        for (int i = 0; i < part.length(); i++) {
            char c = part.charAt(i);
            if (c == '"') inQuotes = !inQuotes;
            if (c == ':' && !inQuotes) return i;
        }
        return -1;
    }

    private static String strip(String raw) {
        String value = raw.trim();
        if (value.startsWith("\"") && value.endsWith("\"") && value.length() >= 2) {
            value = value.substring(1, value.length() - 1);
        }
        return value;
    }
}
