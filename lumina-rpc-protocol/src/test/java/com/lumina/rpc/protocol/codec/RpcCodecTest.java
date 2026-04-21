package com.lumina.rpc.protocol.codec;

import com.lumina.rpc.protocol.RpcMessage;
import com.lumina.rpc.protocol.RpcResponse;
import io.netty.buffer.ByteBuf;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RpcCodecTest {

    @Test
    void encodesAndDecodesJsonResponseMessage() {
        RpcMessage outbound = new RpcMessage();
        outbound.setMagicNumber(RpcMessage.MAGIC_NUMBER);
        outbound.setVersion(RpcMessage.VERSION);
        outbound.setSerializerType(RpcMessage.JSON);
        outbound.setMessageType(RpcMessage.RESPONSE);
        outbound.setRequestId(42L);
        outbound.setBody(RpcResponse.success(42L, "trace-1", "ok"));

        EmbeddedChannel encoder = new EmbeddedChannel(new RpcEncoder());
        assertTrue(encoder.writeOutbound(outbound));
        ByteBuf encoded = encoder.readOutbound();

        EmbeddedChannel decoder = new EmbeddedChannel(new RpcDecoder());
        assertTrue(decoder.writeInbound(encoded));
        RpcMessage inbound = decoder.readInbound();

        assertEquals(RpcMessage.MAGIC_NUMBER, inbound.getMagicNumber());
        assertEquals(RpcMessage.VERSION, inbound.getVersion());
        assertEquals(RpcMessage.JSON, inbound.getSerializerType());
        assertEquals(RpcMessage.RESPONSE, inbound.getMessageType());
        assertEquals(42L, inbound.getRequestId());
        assertInstanceOf(RpcResponse.class, inbound.getBody());

        RpcResponse response = (RpcResponse) inbound.getBody();
        assertEquals(42L, response.getRequestId());
        assertEquals("trace-1", response.getTraceId());
        assertEquals(RpcResponse.SUCCESS, response.getCode());
        assertEquals("ok", response.getData());

        encoder.finishAndReleaseAll();
        decoder.finishAndReleaseAll();
    }
}
