package com.lumina.controlplane.controller;

import com.lumina.controlplane.service.ServiceInstanceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceInstanceControllerTest {

    @Mock
    private ServiceInstanceService serviceInstanceService;

    @InjectMocks
    private ServiceInstanceController controller;

    @Test
    void heartbeatReturnsNotFoundWhenInstanceIsMissing() {
        when(serviceInstanceService.heartbeat("missing-instance")).thenReturn(false);

        ResponseEntity<Void> response = controller.heartbeat("missing-instance");

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void heartbeatReturnsOkWhenInstanceExists() {
        when(serviceInstanceService.heartbeat("known-instance")).thenReturn(true);

        ResponseEntity<Void> response = controller.heartbeat("known-instance");

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }
}
