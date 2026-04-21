package com.lumina.rpc.core.mock;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

final class MockRuleMatcher {

    private static final Logger logger = LoggerFactory.getLogger(MockRuleMatcher.class);

    private final ObjectMapper objectMapper;

    MockRuleMatcher(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    MockRule getMatchingRule(Map<String, List<MockRule>> ruleCache,
                             String serviceName,
                             String methodName,
                             Object[] args) {
        List<MockRule> rules = ruleCache.get(serviceName);

        if (rules == null || rules.isEmpty()) {
            logger.debug("🔍 Mock检查: [{}.{}] -> 规则缓存为空", serviceName, methodName);
            return null;
        }

        logger.info("🔍 Mock检查: [{}.{}] -> 正在匹配规则，参数数量: {}",
                serviceName, methodName, args != null ? args.length : 0);

        List<MockRule> rulesOfThisMethod = rules.stream()
                .filter(rule -> methodName.equals(rule.getMethodName()) || "*".equals(rule.getMethodName()))
                .sorted((r1, r2) -> Integer.compare(r2.getPriority(), r1.getPriority()))
                .toList();

        if (rulesOfThisMethod.isEmpty()) {
            logger.debug("🔍 Mock检查: [{}.{}] -> 无该方法的规则", serviceName, methodName);
            return null;
        }

        for (MockRule rule : rulesOfThisMethod) {
            if (!rule.isEnabled()) {
                logger.debug("🔍 Mock检查: 规则[method={}, priority={}] 已停用，跳过",
                        rule.getMethodName(), rule.getPriority());
                continue;
            }

            if (!rule.hasCondition()) {
                logger.info("✅ 匹配成功: 无条件规则 [method={}, priority={}, type={}]",
                        rule.getMethodName(), rule.getPriority(), rule.getMockType());
                return rule;
            }

            if (evaluateCondition(rule.getConditionRule(), args)) {
                logger.info("✅ 匹配成功: 条件规则 [method={}, priority={}, type={}]",
                        rule.getMethodName(), rule.getPriority(), rule.getMockType());
                return rule;
            }

            logger.debug("🔍 Mock检查: 规则[method={}, priority={}] 条件不匹配，继续检查下一条",
                    rule.getMethodName(), rule.getPriority());
        }

        logger.info("❌ 匹配失败: [{}.{}] 遍历了 {} 条规则，无符合条件的",
                serviceName, methodName, rulesOfThisMethod.size());
        return null;
    }

    private boolean evaluateCondition(String conditionRule, Object[] args) {
        if (conditionRule == null || conditionRule.isEmpty()) {
            return true;
        }

        try {
            JsonNode conditionNode = objectMapper.readTree(conditionRule);

            if (conditionNode.isArray()) {
                for (JsonNode cond : conditionNode) {
                    if (!matchesSingleCondition(cond, args)) {
                        return false;
                    }
                }
                return true;
            }

            if (conditionNode.isObject()) {
                return matchesSingleCondition(conditionNode, args);
            }
        } catch (Exception e) {
            logger.warn("Failed to parse condition rule: {}", conditionRule, e);
        }

        return false;
    }

    @SuppressWarnings("unchecked")
    private boolean matchesSingleCondition(JsonNode cond, Object[] args) {
        if (cond.has("index")) {
            int index = cond.get("index").asInt();
            String expectedValue = cond.has("value") ? cond.get("value").asText() : null;
            String operator = cond.has("operator") ? cond.get("operator").asText() : "equals";

            if (args == null || index >= args.length || index < 0) {
                logger.debug("🔍 Mock检查: 参数[{}] 越界或为空，匹配失败", index);
                return false;
            }

            Object actualValue = args[index];
            logger.info("🔍 Mock检查: 正在对比参数[{}]: 实际值=[{}] (类型:{}) vs 期望值=[{}] (操作符:{})",
                    index,
                    actualValue,
                    actualValue != null ? actualValue.getClass().getSimpleName() : "null",
                    expectedValue,
                    operator);

            return matchesValue(actualValue, expectedValue, operator);
        }

        if (cond.has("argIndex")) {
            int argIndex = cond.get("argIndex").asInt();
            String matchValue = cond.has("matchValue") ? cond.get("matchValue").asText() : null;

            if (args == null || argIndex >= args.length || argIndex < 0) {
                logger.debug("🔍 Mock检查: 参数[{}] 越界或为空，匹配失败", argIndex);
                return false;
            }

            Object actualValue = args[argIndex];
            logger.info("🔍 Mock检查: 正在对比参数[{}]: 实际值=[{}] (类型:{}) vs 期望值=[{}] (操作符:equals)",
                    argIndex,
                    actualValue,
                    actualValue != null ? actualValue.getClass().getSimpleName() : "null",
                    matchValue);

            String actualStr = actualValue != null ? String.valueOf(actualValue) : null;
            boolean result = matchValue != null && matchValue.equals(actualStr);
            logger.info("{} 匹配结果: {}", result ? "✅" : "❌", result);
            return result;
        }

        if (cond.has("field") && cond.has("fieldValue")) {
            String field = cond.get("field").asText();
            String expectedFieldValue = cond.get("fieldValue").asText();
            int paramIndex = cond.has("paramIndex") ? cond.get("paramIndex").asInt() : 0;

            if (args == null || paramIndex >= args.length) {
                return false;
            }

            Object actualFieldValue = getFieldValue(args[paramIndex], field);
            String actualStr = actualFieldValue != null ? String.valueOf(actualFieldValue) : null;
            return expectedFieldValue.equals(actualStr);
        }

        if (cond.has("probability")) {
            double probability = cond.get("probability").asDouble();
            return Math.random() < probability;
        }

        if (cond.has("args")) {
            JsonNode expectedArgs = cond.get("args");
            if (args != null) {
                for (Object arg : args) {
                    if (arg instanceof Map && matchesMapCondition((Map<String, Object>) arg, expectedArgs)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private boolean matchesValue(Object actualValue, String expectedValue, String operator) {
        if (actualValue == null || expectedValue == null) {
            boolean result = actualValue == null && expectedValue == null;
            logger.debug("🔍 参数值对比: 实际值=[{}], 规则值=[{}], 操作符=[{}], 结果=[{}]",
                    actualValue, expectedValue, operator, result);
            return result;
        }

        String actualStr = String.valueOf(actualValue);
        boolean result;

        switch (operator.toLowerCase()) {
            case "equals" -> result = expectedValue.equals(actualStr);
            case "notequals", "not_equals" -> result = !expectedValue.equals(actualStr);
            case "contains" -> result = actualStr.contains(expectedValue);
            case "notcontains", "not_contains" -> result = !actualStr.contains(expectedValue);
            case "startswith", "starts_with" -> result = actualStr.startsWith(expectedValue);
            case "endswith", "ends_with" -> result = actualStr.endsWith(expectedValue);
            case "regex" -> {
                try {
                    result = actualStr.matches(expectedValue);
                } catch (Exception e) {
                    result = false;
                }
            }
            case "gt" -> result = compareDouble(actualStr, expectedValue, 1);
            case "gte" -> result = compareDouble(actualStr, expectedValue, 0);
            case "lt" -> result = compareDouble(actualStr, expectedValue, -1);
            case "lte" -> result = compareDouble(actualStr, expectedValue, -2);
            case "isempty", "is_empty" -> result = actualStr.isEmpty();
            case "isnotempty", "is_not_empty" -> result = !actualStr.isEmpty();
            default -> result = expectedValue.equals(actualStr);
        }

        logger.info("🔍 正在对比：参数的实际值 [{}] 是否匹配规则值 [{}] (操作符: {}), 结果: {}",
                actualStr, expectedValue, operator, result ? "✅ 匹配" : "❌ 不匹配");
        return result;
    }

    private boolean compareDouble(String actualStr, String expectedValue, int mode) {
        try {
            double actual = Double.parseDouble(actualStr);
            double expected = Double.parseDouble(expectedValue);
            return switch (mode) {
                case 1 -> actual > expected;
                case 0 -> actual >= expected;
                case -1 -> actual < expected;
                default -> actual <= expected;
            };
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private Object getFieldValue(Object obj, String fieldPath) {
        if (obj == null) {
            return null;
        }

        String[] parts = fieldPath.split("\\.");
        Object current = obj;

        for (String part : parts) {
            if (current == null) {
                return null;
            }

            if (current instanceof Map) {
                current = ((Map<String, Object>) current).get(part);
            } else {
                try {
                    java.lang.reflect.Field field = current.getClass().getDeclaredField(part);
                    field.setAccessible(true);
                    current = field.get(current);
                } catch (Exception e) {
                    return null;
                }
            }
        }

        return current;
    }

    private boolean matchesMapCondition(Map<String, Object> actual, JsonNode expected) {
        java.util.Iterator<Map.Entry<String, JsonNode>> fields = expected.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            String key = entry.getKey();
            String expectedVal = entry.getValue().asText();

            Object actualVal = actual.get(key);
            String actualStr = actualVal != null ? String.valueOf(actualVal) : null;
            if (!expectedVal.equals(actualStr)) {
                return false;
            }
        }
        return true;
    }
}
