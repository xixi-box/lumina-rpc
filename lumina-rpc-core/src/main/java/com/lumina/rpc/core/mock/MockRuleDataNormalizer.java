package com.lumina.rpc.core.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Normalizes legacy front-end mock response payload shapes.
 */
final class MockRuleDataNormalizer {

    private static final Logger logger = LoggerFactory.getLogger(MockRuleDataNormalizer.class);

    private MockRuleDataNormalizer() {
    }

    static boolean hasFlatKeys(Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            return false;
        }
        return map.keySet().stream().anyMatch(key -> key.contains(".[element"));
    }

    @SuppressWarnings("unchecked")
    static String convertFlatKeysToNestedJson(String jsonStr, ObjectMapper objectMapper) {
        try {
            Map<String, Object> flatMap = objectMapper.readValue(jsonStr, Map.class);
            Map<String, Object> nestedMap = convertFlatKeysMap(flatMap);
            return objectMapper.writeValueAsString(nestedMap);
        } catch (Exception e) {
            logger.warn("🔧 [Mock] 转换扁平化 key 失败，保持原样: {}", e.getMessage());
            return jsonStr;
        }
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> convertFlatKeysMap(Map<String, Object> flatMap) {
        Map<String, Object> result = new LinkedHashMap<>();

        for (Map.Entry<String, Object> entry : flatMap.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (key.contains(".[element")) {
                setNestedValue(result, key, value);
            } else if (value instanceof Map) {
                result.put(key, convertFlatKeysMap((Map<String, Object>) value));
            } else {
                result.put(key, value);
            }
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    private static void setNestedValue(Map<String, Object> root, String path, Object value) {
        String normalizedPath = path.replaceAll("\\.\\[element(\\d+)\\]\\.", "[$1].");
        String[] parts = parsePath(normalizedPath);
        Map<String, Object> current = root;

        for (int i = 0; i < parts.length - 1; i++) {
            String part = parts[i];
            Object next = current.get(part);

            if (next == null) {
                String nextPart = parts[i + 1];
                next = nextPart.matches("\\d+") ? new ArrayList<>() : new LinkedHashMap<>();
                current.put(part, next);
            }

            if (next instanceof List) {
                List<Object> list = (List<Object>) next;
                int index = Integer.parseInt(parts[i + 1]);
                while (list.size() <= index) {
                    list.add(null);
                }
                if (i + 1 == parts.length - 1) {
                    list.set(index, value);
                } else {
                    Object nested = list.get(index);
                    if (nested == null) {
                        String nextNextPart = parts[i + 2];
                        nested = nextNextPart != null && nextNextPart.matches("\\d+")
                                ? new ArrayList<>()
                                : new LinkedHashMap<>();
                        list.set(index, nested);
                    }
                    if (nested instanceof Map) {
                        current = (Map<String, Object>) nested;
                    }
                }
                i++;
            } else if (next instanceof Map) {
                current = (Map<String, Object>) next;
            }
        }

        if (parts.length > 0) {
            current.put(parts[parts.length - 1], value);
        }
    }

    private static String[] parsePath(String path) {
        List<String> parts = new ArrayList<>();
        StringBuilder token = new StringBuilder();

        for (int i = 0; i < path.length(); i++) {
            char c = path.charAt(i);
            if (c == '.') {
                if (token.length() > 0) {
                    parts.add(token.toString());
                    token.setLength(0);
                }
            } else if (c == '[') {
                if (token.length() > 0) {
                    parts.add(token.toString());
                    token.setLength(0);
                }
                int end = path.indexOf(']', i);
                if (end > i) {
                    parts.add(path.substring(i + 1, end));
                    i = end;
                }
            } else {
                token.append(c);
            }
        }

        if (token.length() > 0) {
            parts.add(token.toString());
        }
        return parts.toArray(String[]::new);
    }
}
