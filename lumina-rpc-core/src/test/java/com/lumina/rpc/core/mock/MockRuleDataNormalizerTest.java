package com.lumina.rpc.core.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MockRuleDataNormalizerTest {

    @Test
    @SuppressWarnings("unchecked")
    void convertsFlatElementKeysToNestedArrays() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        String json = """
                {
                  "contacts.[element0].shipId": "A",
                  "contacts.[element1].shipId": "B"
                }
                """;

        String normalizedJson = MockRuleDataNormalizer.convertFlatKeysToNestedJson(json, objectMapper);
        Map<String, Object> normalized = objectMapper.readValue(normalizedJson, Map.class);

        assertTrue(MockRuleDataNormalizer.hasFlatKeys(Map.of("contacts.[element0].shipId", "A")));
        List<Map<String, Object>> contacts = (List<Map<String, Object>>) normalized.get("contacts");
        assertEquals("A", contacts.get(0).get("shipId"));
        assertEquals("B", contacts.get(1).get("shipId"));
    }
}
