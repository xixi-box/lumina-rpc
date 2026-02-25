package com.lumina.rpc.core.spi;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.lumina.rpc.core.protocol.RpcMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * JSON 序列化器实现
 */
public class JsonSerializer implements Serializer {

    private static final Logger logger = LoggerFactory.getLogger(JsonSerializer.class);

    private final ObjectMapper objectMapper;

    public JsonSerializer() {
        this.objectMapper = new ObjectMapper();
        // 禁用日期写入为时间戳，便于阅读
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        // 在 JSON 中包含类型信息，以便正确反序列化
        // this.objectMapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
    }

    @Override
    public byte[] serialize(Object obj) {
        try {
            return objectMapper.writeValueAsBytes(obj);
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize object", e);
            throw new RuntimeException("Serialization failed", e);
        }
    }

    @Override
    public <T> T deserialize(byte[] bytes, Class<T> clazz) {
        try {
            return objectMapper.readValue(bytes, clazz);
        } catch (JsonProcessingException e) {
            logger.error("Failed to deserialize bytes to {}", clazz.getName(), e);
            throw new RuntimeException("Deserialization failed", e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public byte getType() {
        return RpcMessage.JSON;
    }

    @Override
    public String getName() {
        return "json";
    }
}
