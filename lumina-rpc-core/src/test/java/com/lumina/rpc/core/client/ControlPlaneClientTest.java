package com.lumina.rpc.core.client;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ControlPlaneClientTest {

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
        ControlPlaneClient.reset();
    }

    @Test
    void heartbeatFailureMarksClientUnregisteredForRetry() throws Exception {
        AtomicInteger registerCalls = new AtomicInteger();
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/api/v1/registry/register", exchange -> {
            registerCalls.incrementAndGet();
            sendResponse(exchange, 201, "{}");
        });
        server.createContext("/api/v1/registry/heartbeat/demo.Service@127.0.0.1:9001", exchange ->
                sendResponse(exchange, 500, "{}"));
        server.start();

        String controlPlaneUrl = "http://localhost:" + server.getAddress().getPort();
        ControlPlaneClient.initialize(controlPlaneUrl, 30, 10, 5, 2);
        ControlPlaneClient client = ControlPlaneClient.getInstance();
        client.register("demo.Service", "127.0.0.1", 9001, "1.0.0", null);

        assertTrue(client.isRegistered());

        Method sendHeartbeat = ControlPlaneClient.class.getDeclaredMethod("sendHeartbeat");
        sendHeartbeat.setAccessible(true);
        sendHeartbeat.invoke(client);

        assertFalse(client.isRegistered());
        assertTrue(registerCalls.get() >= 1);
    }

    private void sendResponse(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
