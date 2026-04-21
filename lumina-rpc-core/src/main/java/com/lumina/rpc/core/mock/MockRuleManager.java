package com.lumina.rpc.core.mock;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Mock 规则管理器（手术刀型）
 *
 * 管理 RPC 服务的动态降级规则，支持通过 SSE 实时更新
 *
 * 企业级特性：
 * 1. 多条件匹配：支持多参数组合匹配（AND 关系）
 * 2. 双模引擎：SHORT_CIRCUIT（直接阻断）和 TAMPER（篡改真实数据）
 * 3. 占位符篡改：支持 {{base}} 占位符，保留原始值
 *
 * @author Lumina-RPC Team
 * @since 1.0.0
 */
public class MockRuleManager {

    private static final Logger logger = LoggerFactory.getLogger(MockRuleManager.class);

    private static final MockRuleManager INSTANCE = new MockRuleManager();

    // ObjectMapper 用于 JSON 解析和数据合并
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final MockRuleMatcher matcher = new MockRuleMatcher(objectMapper);

    // Mock 规则缓存: serviceName -> List<MockRule>（支持同一方法的多个规则，按优先级排序）
    private final ConcurrentHashMap<String, List<MockRule>> ruleCache = new ConcurrentHashMap<>();

    private MockRuleManager() {
        // 单例模式
    }

    public static MockRuleManager getInstance() {
        return INSTANCE;
    }

    /**
     * 清空所有规则（用于测试或重置）
     */
    public void clearAllRules() {
        ruleCache.clear();
        logger.info("All mock rules cleared");
    }

    /**
     * 重置单例（仅用于测试）
     */
    public static void reset() {
        INSTANCE.clearAllRules();
    }

    /**
     * 检查是否有启用的 Mock 规则
     */
    public boolean hasMockRule(String serviceName, String methodName) {
        List<MockRule> rules = ruleCache.get(serviceName);
        if (rules == null || rules.isEmpty()) {
            return false;
        }
        // 检查是否有启用的规则匹配该方法
        return rules.stream()
                .filter(MockRule::isEnabled)
                .anyMatch(rule -> methodName.equals(rule.getMethodName()) || "*".equals(rule.getMethodName()));
    }

    /**
     * 获取匹配的规则（严格遵循遍历控制流）
     *
     * 匹配逻辑：
     * 1. 忽略停用的规则 (enabled == false)
     * 2. 无条件规则直接命中返回
     * 3. 有条件规则进行参数比对，不匹配时 continue 继续看下一条
     * 4. 遍历结束未命中返回 null，让外层发起真实请求
     *
     * @param serviceName 服务名
     * @param methodName  方法名
     * @param args        调用参数
     * @return 命中的规则，未命中返回 null
     */
    public MockRule getMatchingRule(String serviceName, String methodName, Object[] args) {
        return matcher.getMatchingRule(ruleCache, serviceName, methodName, args);
    }

    /**
     * 执行 Mock 短路模式（带参数）
     * @param serviceName 服务名
     * @param methodName 方法名
     * @param args 实际调用参数（用于条件匹配）
     * @param returnType 返回类型
     */
    public Object executeMock(String serviceName, String methodName, Object[] args, Class<?> returnType) {
        MockRule rule = getMatchingRule(serviceName, methodName, args);
        if (rule == null) {
            return null;
        }
        return executeShortCircuit(rule, serviceName, methodName, returnType);
    }

    /**
     * 执行 Mock 短路模式（兼容旧版本，不带参数）
     * @deprecated 请使用 executeMock(serviceName, methodName, args, returnType)
     */
    @Deprecated
    public Object executeMock(String serviceName, String methodName, Class<?> returnType) {
        return executeMock(serviceName, methodName, null, returnType);
    }

    /**
     * 执行短路模式 Mock（增强版：支持部分字段填充）
     *
     * 关键特性：
     * 1. 如果用户只填写了部分字段（如 {"temperature": 100}），自动创建完整 DTO 并只覆盖指定字段
     * 2. 绝对不返回 null（除非返回类型是 void）
     */
    private Object executeShortCircuit(MockRule rule, String serviceName, String methodName, Class<?> returnType) {
        logger.info("⚡ 命中动态降级规则，短路网络请求: {}.{} [mode=SHORT_CIRCUIT]", serviceName, methodName);

        // 延迟处理
        if (rule.getDelayMs() > 0) {
            try {
                Thread.sleep(rule.getDelayMs());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // 增强容错：如果配置了抛出异常，改为返回默认响应（严禁抛异常！）
        if (rule.isThrowException()) {
            logger.warn("⚠️ Mock 规则配置了抛出异常，但为了保护业务，请求将被拦截并返回默认响应");
            return createDefaultResponse(returnType);
        }

        Object responseData = rule.getResponseData();

        // 空值校验：如果 mockValue 为空，尝试返回合适的空对象
        if (responseData == null || (responseData instanceof String && ((String) responseData).isEmpty())) {
            logger.warn("⚠️ Mock 数据为空，尝试创建默认响应");
            return createDefaultResponse(returnType);
        }

        // 尝试转换类型（支持部分字段：JSON 中有的字段会被填充，没有的字段保持默认值）
        try {
            Object result = convertToType(responseData, returnType);
            if (result != null) {
                logger.info("✅ Mock 转换成功，返回类型: {}", returnType.getName());
                return result;
            }
            // 如果转换结果为 null，尝试创建默认响应
            return createDefaultResponse(returnType);
        } catch (Exception e) {
            logger.error("❌ Mock 转换失败，原因: {}, 尝试创建默认响应", e.getMessage(), e);
            return createDefaultResponse(returnType);
        }
    }

    /**
     * 根据返回类型创建默认响应
     */
    private Object createDefaultResponse(Class<?> returnType) {
        if (returnType == null || returnType == void.class || returnType == Void.class) {
            return null;
        }

        // 常见基础类型的默认值
        if (returnType == String.class) {
            return "";
        }
        if (returnType == int.class || returnType == Integer.class) {
            return 0;
        }
        if (returnType == long.class || returnType == Long.class) {
            return 0L;
        }
        if (returnType == boolean.class || returnType == Boolean.class) {
            return false;
        }
        if (returnType == double.class || returnType == Double.class) {
            return 0.0;
        }
        if (returnType == float.class || returnType == Float.class) {
            return 0.0f;
        }

        // 对于复杂类型，尝试使用反射创建空对象
        try {
            // 首先尝试无参构造函数
            if (hasNoArgConstructor(returnType)) {
                return returnType.getDeclaredConstructor().newInstance();
            }
        } catch (Exception e) {
            logger.debug("无法通过无参构造函数创建 {}: {}", returnType.getName(), e.getMessage());
        }

        // 尝试创建 Map 类型（常见的返回类型）
        if (Map.class.isAssignableFrom(returnType)) {
            return new java.util.HashMap<>();
        }

        // 尝试创建 List 类型
        if (List.class.isAssignableFrom(returnType)) {
            return new java.util.ArrayList<>();
        }

        // 无法创建时返回 null，并在日志中提示
        logger.warn("⚠️ 无法为类型 {} 创建默认响应，返回 null", returnType.getName());
        return null;
    }

    /**
     * 检查是否有无参构造函数
     */
    private boolean hasNoArgConstructor(Class<?> clazz) {
        try {
            clazz.getDeclaredConstructor();
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    /**
     * 执行篡改模式 Mock（升级版）
     *
     * 支持：
     * 1. JSON 字段合并
     * 2. 占位符替换（{{base}}）
     * 3. 字段级精准篡改
     */
    public Object executeTamper(MockRule rule, String serviceName, String methodName,
                                Object realResponse, Class<?> returnType) {
        logger.info("🔄 命中数据篡改规则，合并 Mock 数据: {}.{} [mode=TAMPER]", serviceName, methodName);

        // 安全检查：如果真实响应为 null，直接返回 null（不做篡改）
        if (realResponse == null) {
            logger.warn("⚠️ 真实响应为 null，TAMPER 模式无法合并，返回 null");
            return null;
        }

        try {
            // 将真实响应转换为 JSON Node
            JsonNode realNode = objectMapper.valueToTree(realResponse);

            // 获取 Mock 数据
            Object mockData = rule.getResponseData();

            // 容错：Mock 数据为空时直接返回真实响应
            if (mockData == null || (mockData instanceof String && ((String) mockData).isEmpty())) {
                logger.warn("⚠️ Mock 数据为空，TAMPER 模式回退至真实响应");
                return realResponse;
            }

            JsonNode mockNode;
            if (mockData instanceof String) {
                try {
                    mockNode = objectMapper.readTree((String) mockData);
                } catch (Exception e) {
                    logger.error("❌ 解析 Mock JSON 失败: {}, 回退至真实响应", e.getMessage());
                    return realResponse;
                }
            } else {
                mockNode = objectMapper.valueToTree(mockData);
            }

            // 容错：mockNode 为空时返回真实响应
            if (mockNode == null || mockNode.isNull()) {
                logger.warn("⚠️ Mock 节点为空，TAMPER 模式回退至真实响应");
                return realResponse;
            }

            // 递归合并，并处理占位符
            JsonNode mergedNode = mergeJsonWithPlaceholders(realNode, mockNode);

            // 转换为目标类型 - 增强版，支持复杂类型
            try {
                return convertJsonNodeToTargetType(mergedNode, returnType, realResponse);
            } catch (Exception e) {
                logger.error("❌ TAMPER 模式类型转换失败: {}, 回退至真实响应", e.getMessage());
                return realResponse;
            }

        } catch (Exception e) {
            // 关键：任何异常都不应该破坏业务请求，必须返回原始真实数据
            logger.error("❌ TAMPER 模式合并失败: {}, 回退至真实响应", e.getMessage(), e);
            return realResponse;
        }
    }

    /**
     * 将 JsonNode 转换为目标类型，支持复杂类型和泛型
     *
     * @param node 要转换的 JSON 节点
     * @param targetType 目标类型
     * @param originalObject 原始对象（用于推断泛型信息）
     * @return 转换后的对象
     */
    private Object convertJsonNodeToTargetType(JsonNode node, Class<?> targetType, Object originalObject) throws Exception {
        // 基本情况处理
        if (node == null || node.isNull()) {
            return null;
        }

        // 如果目标类型就是 JsonNode，直接返回
        if (targetType == JsonNode.class) {
            return node;
        }

        // 尝试标准转换
        try {
            return objectMapper.treeToValue(node, targetType);
        } catch (Exception e) {
            // 标准转换失败，尝试更智能的转换
            logger.debug("标准 treeToValue 转换失败，尝试智能转换: {}", e.getMessage());
        }

        // 智能转换策略

        // 1. 如果原始对象存在且类型匹配，尝试将 node 转换为该类型
        if (originalObject != null) {
            Class<?> originalClass = originalObject.getClass();

            // 如果目标类型是原始对象的类型或父类/接口
            if (targetType.isAssignableFrom(originalClass)) {
                try {
                    // 尝试转换为原始对象的具体类型
                    Object result = objectMapper.treeToValue(node, originalClass);
                    if (result != null && targetType.isInstance(result)) {
                        return result;
                    }
                } catch (Exception ignored) {
                    // 继续尝试其他方法
                }
            }

            // 2. 处理集合类型 - 尝试保留原始集合的泛型信息
            if (Collection.class.isAssignableFrom(targetType) && originalObject instanceof Collection) {
                return convertToCollection(node, targetType, (Collection<?>) originalObject);
            }

            // 3. 处理 Map 类型
            if (Map.class.isAssignableFrom(targetType) && originalObject instanceof Map) {
                return convertToMap(node, targetType, (Map<?, ?>) originalObject);
            }
        }

        // 4. 处理集合类型（无原始对象信息时）
        if (Collection.class.isAssignableFrom(targetType)) {
            return convertToCollection(node, targetType, null);
        }

        // 5. 处理 Map 类型（无原始对象信息时）
        if (Map.class.isAssignableFrom(targetType)) {
            return convertToMap(node, targetType, null);
        }

        // 6. 尝试使用 convertValue 作为最后手段
        try {
            return objectMapper.convertValue(node, targetType);
        } catch (Exception e) {
            logger.debug("convertValue 也失败了: {}", e.getMessage());
        }

        // 所有方法都失败了，抛出异常
        throw new IllegalArgumentException("无法将 JSON 转换为类型: " + targetType.getName());
    }

    /**
     * 转换 JsonNode 为 Collection 类型
     */
    @SuppressWarnings("unchecked")
    private Object convertToCollection(JsonNode node, Class<?> targetType, Collection<?> originalCollection) throws Exception {
        if (!node.isArray()) {
            throw new IllegalArgumentException("Expected array node for Collection type");
        }

        // 如果没有原始集合信息，使用默认的 List 类型
        if (originalCollection == null) {
            return objectMapper.treeToValue(node, List.class);
        }

        // 尝试推断元素类型
        Class<?> elementType = Object.class;
        if (!originalCollection.isEmpty()) {
            // 获取第一个元素的类型作为推断
            elementType = originalCollection.iterator().next().getClass();
        }

        // 构建带泛型信息的 JavaType
        JavaType javaType = objectMapper.getTypeFactory()
                .constructCollectionType((Class<? extends Collection>) targetType, elementType);

        return objectMapper.readValue(node.toString(), javaType);
    }

    /**
     * 转换 JsonNode 为 Map 类型
     */
    @SuppressWarnings("unchecked")
    private Object convertToMap(JsonNode node, Class<?> targetType, Map<?, ?> originalMap) throws Exception {
        if (!node.isObject()) {
            throw new IllegalArgumentException("Expected object node for Map type");
        }

        // 如果没有原始 Map 信息，使用默认的 Map 类型
        if (originalMap == null) {
            return objectMapper.treeToValue(node, Map.class);
        }

        // 尝试推断键值类型
        Class<?> keyType = String.class;
        Class<?> valueType = Object.class;

        if (!originalMap.isEmpty()) {
            Map.Entry<?, ?> firstEntry = originalMap.entrySet().iterator().next();
            keyType = firstEntry.getKey().getClass();
            valueType = firstEntry.getValue().getClass();
        }

        // 构建带泛型信息的 JavaType
        JavaType javaType = objectMapper.getTypeFactory()
                .constructMapType((Class<? extends Map>) targetType, keyType, valueType);

        return objectMapper.readValue(node.toString(), javaType);
    }

    /**
     * 递归合并两个 JSON Node，支持占位符
     *
     * 占位符格式：
     * - {{base}}: 整个原始值
     * - {{base.field}}: 原始值的某个字段
     * - {{base[0]}}: 原始数组的某个元素
     *
     * 示例：
     * 如果真实返回 {"status": "OK"}，Mock 值为 {"status": "{{base}} (Tampered)"}
     * 则最终返回 {"status": "OK (Tampered)"}
     */
    private JsonNode mergeJsonWithPlaceholders(JsonNode base, JsonNode overlay) {
        if (overlay == null || overlay.isNull()) {
            return base;
        }

        if (base == null || base.isNull()) {
            return processPlaceholders(overlay, null);
        }

        if (!base.isObject() || !overlay.isObject()) {
            // 非对象类型，处理占位符后覆盖
            return processPlaceholders(overlay, base);
        }

        ObjectNode result = (ObjectNode) base.deepCopy();
        overlay.fields().forEachRemaining(entry -> {
            String fieldName = entry.getKey();
            JsonNode overlayValue = entry.getValue();

            // 关键修复：忽略空值覆盖
            // 只有当 Mock 值不为 null、不为空字符串、且不是全空对象时才覆盖
            if (isEmptyValue(overlayValue)) {
                // 跳过空值，保留真实值
                return;
            }

            if (result.has(fieldName) && result.get(fieldName).isObject() && overlayValue.isObject()) {
                // 递归合并嵌套对象
                result.set(fieldName, mergeJsonWithPlaceholders(result.get(fieldName), overlayValue));
            } else {
                // 处理占位符后设置
                JsonNode processedValue = processPlaceholders(overlayValue, result.get(fieldName));
                result.set(fieldName, processedValue);
            }
        });

        return result;
    }

    /**
     * 判断 JSON Node 是否为空值（null、空字符串、空对象）
     * 空值不应该覆盖真实值
     */
    private boolean isEmptyValue(JsonNode node) {
        if (node == null || node.isNull()) {
            return true;
        }
        // 空字符串
        if (node.isTextual() && node.asText().isEmpty()) {
            return true;
        }
        // 空数组
        if (node.isArray() && node.isEmpty()) {
            return true;
        }
        // 空对象（没有字段）
        if (node.isObject() && node.isEmpty()) {
            return true;
        }
        return false;
    }

    /**
     * 处理占位符
     *
     * 支持：
     * - {{base}}: 原始值（所有类型）
     * - {{base}}+100: 数字加法（仅限数字类型）
     * - {{base}}-50: 数字减法（仅限数字类型）
     * - {{base}}*2: 数字乘法（仅限数字类型）
     * - {{base}}/10: 数字除法（仅限数字类型）
     *
     * 关键：严格区分字符串和数字类型，互不干扰！
     */
    private JsonNode processPlaceholders(JsonNode node, JsonNode baseValue) {
        if (node == null) {
            return node;
        }

        // 处理字符串节点中的占位符
        if (node.isTextual()) {
            String text = node.asText();

            if (text.contains("{{base}}")) {
                // ================== 第一步：严格判断原始数据类型 ==================
                boolean isNumericType = false;
                boolean isIntegerType = false;
                boolean isDoubleType = false;
                double numericValue = 0;
                String stringValue = "";

                if (baseValue != null && !baseValue.isNull()) {
                    if (baseValue.isNumber()) {
                        // 原始类型是数字：int, long, double, float
                        isNumericType = true;
                        numericValue = baseValue.asDouble();
                        isIntegerType = baseValue.isInt() || baseValue.isLong();
                        isDoubleType = baseValue.isDouble() || baseValue.isFloat();
                        stringValue = String.valueOf(numericValue);
                    } else if (baseValue.isTextual()) {
                        // 原始类型是字符串
                        stringValue = baseValue.asText();
                        // 尝试解析为数字
                        try {
                            numericValue = Double.parseDouble(stringValue);
                            isNumericType = true;
                            isIntegerType = !stringValue.contains(".");
                            isDoubleType = stringValue.contains(".");
                        } catch (NumberFormatException e) {
                            // 解析失败，保持为字符串
                            isNumericType = false;
                        }
                    } else if (baseValue.isBoolean()) {
                        stringValue = String.valueOf(baseValue.asBoolean());
                    } else {
                        stringValue = baseValue.toString();
                    }
                }

                // ================== 第二步：严格分支处理 ==================

                // 分支 A：数字类型 + 严格的数学运算正则
                if (isNumericType && text.matches("^\\{\\{base\\}\\}\\s*[+\\-*/]\\s*\\d+(\\.\\d+)?$")) {
                    try {
                        return processMathOperation(text, numericValue, isIntegerType, isDoubleType);
                    } catch (Exception e) {
                        logger.error("❌ 数学运算失败: template={}, realValue={}, error={}", text, numericValue, e.getMessage());
                        // 运算失败，返回原始值
                        return baseValue;
                    }
                }

                // 分支 B：所有其他情况（包括字符串类型）- 简单字符串替换
                String resolved = text.replace("{{base}}", stringValue);
                return TextNode.valueOf(resolved);
            }
            return node;
        }

        // 处理对象节点
        if (node.isObject()) {
            ObjectNode result = (ObjectNode) node.deepCopy();
            result.fields().forEachRemaining(entry -> {
                // 传递当前字段对应的真实值，用于子字段的 {{base}} 替换
                JsonNode childBaseValue = baseValue != null && baseValue.isObject()
                    ? baseValue.get(entry.getKey()) : baseValue;
                JsonNode processed = processPlaceholders(entry.getValue(), childBaseValue);
                result.set(entry.getKey(), processed);
            });
            return result;
        }

        return node;
    }

    /**
     * 处理数学运算 - 仅处理纯数字
     */
    private JsonNode processMathOperation(String template, double realValue, boolean isIntegerType, boolean isDoubleType) {
        logger.info("🔢 [Math] 收到数学运算请求: template={}, realValue={}, isInteger={}, isDouble={}",
                template, realValue, isIntegerType, isDoubleType);

        // 极度严格的正则：{{base}}+数字
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("^\\{\\{base\\}\\}\\s*([+\\-*/])\\s*(\\d+(\\.\\d+)?)$");
        java.util.regex.Matcher matcher = pattern.matcher(template);

        if (!matcher.find()) {
            logger.warn("⚠️ [Math] 正则不匹配，降级为字符串替换: {}", template);
            return TextNode.valueOf(template.replace("{{base}}", String.valueOf(realValue)));
        }

        String operator = matcher.group(1);
        String operandStr = matcher.group(2);
        double operand;
        try {
            operand = Double.parseDouble(operandStr);
        } catch (NumberFormatException e) {
            logger.error("❌ [Math] 操作数解析失败: {}", operandStr);
            return TextNode.valueOf(template.replace("{{base}}", String.valueOf(realValue)));
        }

        logger.info("🔢 [Math] 解析成功: operator={}, operand={}", operator, operand);

        // 执行计算
        double result = realValue;
        switch (operator) {
            case "+":
                result = realValue + operand;
                break;
            case "-":
                result = realValue - operand;
                break;
            case "*":
                result = realValue * operand;
                break;
            case "/":
                if (operand != 0) {
                    result = realValue / operand;
                } else {
                    logger.error("❌ [Math] 除数不能为0");
                    return TextNode.valueOf(template.replace("{{base}}", String.valueOf(realValue)));
                }
                break;
            default:
                logger.warn("⚠️ [Math] 未知操作符: {}", operator);
        }

        logger.info("🔢 [Math] 计算结果: {} {} {} = {}", realValue, operator, operand, result);

        // 根据原始类型返回正确类型的 JsonNode
        if (isDoubleType || (!isIntegerType && result != Math.floor(result))) {
            return new com.fasterxml.jackson.databind.node.DoubleNode(result);
        } else {
            return new com.fasterxml.jackson.databind.node.LongNode((long) Math.round(result));
        }
    }

    /**
     * 递归合并两个 JSON Node（旧方法，保留兼容）
     */
    private JsonNode mergeJson(JsonNode base, JsonNode overlay) {
        return mergeJsonWithPlaceholders(base, overlay);
    }

    /**
     * 类型转换
     */
    private Object convertToType(Object data, Class<?> targetType) {
        if (data == null) {
            return null;
        }

        if (targetType == null) {
            logger.warn("⚠️ 目标类型为 null，直接返回原数据");
            return data;
        }

        // 如果已经可以赋值，直接返回
        if (targetType.isAssignableFrom(data.getClass())) {
            return data;
        }

        // 处理简单类型转复杂类型的情况
        // 例如：用户填了 "true" 但方法要求返回 DTO
        if (isSimpleValue(data) && isComplexType(targetType)) {
            logger.warn("⚠️ Mock 数据是简单值 {}，目标类型是复杂类型 {}，尝试包装",
                    data, targetType.getName());
            return wrapSimpleValue(data, targetType);
        }

        try {
            return objectMapper.convertValue(data, targetType);
        } catch (Exception e) {
            // 增强错误日志：说明到底哪个字段转不动
            logger.error("❌ Mock 转换失败！源数据类型: {} -> 目标类型: {}，原因: {}",
                    data.getClass().getName(), targetType.getName(), e.getMessage());
            logger.error("❌ 原始数据内容: {}", data);

            // 尝试返回原始数据（不做转换）
            return data;
        }
    }

    /**
     * 判断是否为简单值
     */
    private boolean isSimpleValue(Object obj) {
        if (obj == null) return true;
        Class<?> clazz = obj.getClass();
        return clazz == String.class ||
               clazz == Integer.class ||
               clazz == Long.class ||
               clazz == Boolean.class ||
               clazz == Double.class ||
               clazz == Float.class ||
               clazz == int.class ||
               clazz == long.class ||
               clazz == boolean.class ||
               clazz == double.class ||
               clazz == float.class;
    }

    /**
     * 判断是否为复杂类型（不是基础类型和常见集合）
     */
    private boolean isComplexType(Class<?> clazz) {
        if (clazz == null) return false;
        // 基础类型和包装类型
        if (clazz.isPrimitive() ||
            clazz == String.class ||
            clazz == Integer.class ||
            clazz == Long.class ||
            clazz == Boolean.class ||
            clazz == Double.class ||
            clazz == Float.class ||
            clazz == void.class ||
            clazz == Void.class) {
            return false;
        }
        // 常见集合类型
        if (Collection.class.isAssignableFrom(clazz) ||
            Map.class.isAssignableFrom(clazz)) {
            return false;
        }
        return true;
    }

    /**
     * 将简单值包装进复杂类型
     * 尝试找到目标类型的核心字段并赋值
     */
    private Object wrapSimpleValue(Object simpleValue, Class<?> targetType) {
        try {
            // 尝试创建目标类型的实例
            Object target = targetType.getDeclaredConstructor().newInstance();

            // 查找常见的"值"字段并赋值
            String[] commonValueFields = {"value", "data", "result", "message", "status", "code", "info"};

            for (String fieldName : commonValueFields) {
                try {
                    java.lang.reflect.Field field = targetType.getDeclaredField(fieldName);
                    field.setAccessible(true);
                    // 尝试找到兼容的类型
                    Class<?> fieldType = field.getType();
                    if (fieldType.isAssignableFrom(simpleValue.getClass())) {
                        field.set(target, simpleValue);
                        logger.info("✅ 成功将简单值 {} 包装到字段 {} 中", simpleValue, fieldName);
                        return target;
                    }
                } catch (NoSuchFieldException ignored) {
                    // 字段不存在，继续尝试下一个
                }
            }

            // 如果没找到匹配的字段，返回原始简单值（让调用方处理类型不匹配）
            logger.warn("⚠️ 无法将简单值 {} 包装到类型 {} 中，返回原始值",
                    simpleValue, targetType.getName());
            return simpleValue;

        } catch (Exception e) {
            logger.warn("⚠️ 包装简单值失败: {}", e.getMessage());
            return simpleValue;
        }
    }

    /**
     * 获取匹配的第一条启用的 Mock 规则（按优先级排序）
     *
     * @deprecated 请使用 getMatchingRule(serviceName, methodName, args) 进行完整匹配
     */
    @Deprecated
    public MockRule getMockRule(String serviceName, String methodName) {
        List<MockRule> rules = ruleCache.get(serviceName);
        if (rules == null || rules.isEmpty()) {
            return null;
        }
        // 返回匹配该方法的第一条启用的规则（按优先级降序）
        return rules.stream()
                .filter(MockRule::isEnabled)
                .filter(rule -> methodName.equals(rule.getMethodName()) || "*".equals(rule.getMethodName()))
                .max((r1, r2) -> Integer.compare(r1.getPriority(), r2.getPriority()))
                .orElse(null);
    }

    /**
     * 添加或更新 Mock 规则
     *
     * 注意：这会添加一条新规则，如果存在相同 methodName 的规则不会覆盖，而是共存（按优先级匹配）
     */
    public void addMockRule(String serviceName, MockRule rule) {
        List<MockRule> rules = ruleCache.computeIfAbsent(serviceName, k -> new CopyOnWriteArrayList<>());
        rules.add(rule);
        logger.info("📝 Mock rule added: {}.{} (priority={}, enabled={})",
                serviceName, rule.getMethodName(), rule.getPriority(), rule.isEnabled());
    }

    /**
     * 添加或更新 Mock 规则（兼容旧版本）
     *
     * @deprecated 请使用 addMockRule(String serviceName, MockRule rule)
     */
    @Deprecated
    public void addMockRule(String serviceName, String methodName, MockRule rule) {
        rule.setMethodName(methodName);
        addMockRule(serviceName, rule);
    }

    /**
     * 删除指定服务的所有 Mock 规则
     */
    public void removeMockRule(String serviceName, String methodName) {
        List<MockRule> rules = ruleCache.get(serviceName);
        if (rules != null) {
            boolean removed = rules.removeIf(rule -> methodName.equals(rule.getMethodName()));
            if (removed) {
                logger.info("🗑️ Mock rules removed: {}.{} (剩余 {} 条规则)", serviceName, methodName, rules.size());
            }
        }
    }

    /**
     * 清除指定服务的所有 Mock 规则
     */
    public void clearServiceRules(String serviceName) {
        List<MockRule> removed = ruleCache.remove(serviceName);
        if (removed != null && !removed.isEmpty()) {
            logger.info("🗑️ Cleared {} mock rules for service: {}", removed.size(), serviceName);
        }
    }

    /**
     * 清除所有 Mock 规则
     */
    public void clearAll() {
        int totalRules = ruleCache.values().stream()
                .mapToInt(List::size)
                .sum();
        ruleCache.clear();
        logger.info("🗑️ Cleared all {} mock rules", totalRules);
    }

    /**
     * 从 Control Plane 同步的规则数据更新本地缓存
     */
    public void updateRulesFromControlPlane(String serviceName, List<Map<String, Object>> rules) {
        if (rules == null || rules.isEmpty()) {
            clearServiceRules(serviceName);
            return;
        }

        List<MockRule> newRules = new CopyOnWriteArrayList<>();
        for (Map<String, Object> ruleData : rules) {
            try {
                MockRule rule = parseRule(ruleData);
                if (rule != null) {
                    newRules.add(rule);
                }
            } catch (Exception e) {
                logger.warn("Failed to parse mock rule: {}", ruleData, e);
            }
        }

        ruleCache.put(serviceName, newRules);
        logger.info("🔄 Updated {} mock rules for service: {} (enabled: {})",
                newRules.size(), serviceName,
                newRules.stream().filter(MockRule::isEnabled).count());
    }

    /**
     * 解析规则数据
     */
    @SuppressWarnings("unchecked")
    private MockRule parseRule(Map<String, Object> data) {
        MockRule rule = new MockRule();

        rule.setMethodName((String) data.getOrDefault("methodName", "*"));
        rule.setEnabled((Boolean) data.getOrDefault("enabled", true));
        rule.setDelayMs(((Number) data.getOrDefault("responseDelayMs", 0)).longValue());

        // 解析条件规则（支持新格式）
        String conditionRule = (String) data.get("conditionRule");
        if (conditionRule != null && !conditionRule.isEmpty()) {
            rule.setConditionRule(conditionRule);
        }

        // 解析 Mock 类型
        String mockTypeStr = (String) data.getOrDefault("mockType", "SHORT_CIRCUIT");
        try {
            rule.setMockType(MockRule.MockType.valueOf(mockTypeStr.toUpperCase()));
        } catch (Exception e) {
            rule.setMockType(MockRule.MockType.SHORT_CIRCUIT);
        }

        // 响应数据
        Object responseBody = data.get("responseBody");
        if (responseBody instanceof String) {
            String responseStr = (String) responseBody;
            // 兼容处理：检查是否包含前端错误生成的 [elementN] 格式的 key
            if (responseStr.contains(".[element")) {
                responseStr = MockRuleDataNormalizer.convertFlatKeysToNestedJson(responseStr, objectMapper);
                logger.info("🔧 [Mock] 检测到扁平化 key，已转换为嵌套 JSON");
            }
            try {
                rule.setResponseData(objectMapper.readValue(responseStr, Object.class));
                rule.setResponseDataJson(responseStr);
            } catch (Exception e) {
                rule.setResponseData(responseStr);
            }
        } else if (responseBody instanceof Map) {
            // 兼容处理：检查 Map 是否包含错误格式的 key
            Map<String, Object> responseMap = (Map<String, Object>) responseBody;
            if (MockRuleDataNormalizer.hasFlatKeys(responseMap)) {
                responseMap = MockRuleDataNormalizer.convertFlatKeysMap(responseMap);
                try {
                    rule.setResponseDataJson(objectMapper.writeValueAsString(responseMap));
                } catch (Exception ignored) {}
                rule.setResponseData(responseMap);
            } else {
                rule.setResponseData(responseMap);
                try {
                    rule.setResponseDataJson(objectMapper.writeValueAsString(responseMap));
                } catch (Exception ignored) {}
            }
        }

        // 异常配置
        String responseType = (String) data.getOrDefault("responseType", "SUCCESS");
        rule.setThrowException("ERROR".equalsIgnoreCase(responseType) || "EXCEPTION".equalsIgnoreCase(responseType));
        rule.setExceptionMessage((String) data.getOrDefault("errorMessage", "Mock exception"));

        // 优先级
        if (data.containsKey("priority")) {
            rule.setPriority(((Number) data.get("priority")).intValue());
        }

        return rule;
    }

    /**
     * 获取所有 Mock 规则
     */
    public Map<String, List<MockRule>> getAllRules() {
        return new ConcurrentHashMap<>(ruleCache);
    }

    /**
     * 获取 Mock 规则数量
     */
    public int getRuleCount() {
        return ruleCache.values().stream()
                .mapToInt(List::size)
                .sum();
    }
}
