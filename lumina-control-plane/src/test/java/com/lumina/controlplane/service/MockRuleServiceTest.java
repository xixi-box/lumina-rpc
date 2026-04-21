package com.lumina.controlplane.service;

import com.lumina.controlplane.entity.MockRuleEntity;
import com.lumina.controlplane.exception.NotFoundException;
import com.lumina.controlplane.mapper.MockRuleMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MockRuleServiceTest {

    @Mock
    private MockRuleMapper mapper;

    @Mock
    private SseBroadcastService sseBroadcastService;

    @InjectMocks
    private MockRuleService mockRuleService;

    @Test
    void createRuleAppliesDefaultsAndBroadcastsCreate() {
        MockRuleEntity rule = new MockRuleEntity();
        rule.setServiceName("demo.Service");
        rule.setMethodName("call");

        MockRuleEntity created = mockRuleService.createRule(rule);

        assertEquals(true, created.getEnabled());
        assertEquals(0, created.getPriority());
        assertEquals(0, created.getResponseDelayMs());
        assertEquals(200, created.getHttpStatus());
        verify(mapper).insert(rule);
        verify(sseBroadcastService).broadcastRuleChange("demo.Service", created.getId(), "CREATE");
    }

    @Test
    void updateRuleThrowsNotFoundWhenRuleDoesNotExist() {
        when(mapper.selectOneById(99L)).thenReturn(null);

        assertThrows(NotFoundException.class, () -> mockRuleService.updateRule(99L, new MockRuleEntity()));
    }

    @Test
    void toggleEnabledPersistsAndBroadcastsAction() {
        MockRuleEntity rule = new MockRuleEntity();
        rule.setId(7L);
        rule.setServiceName("demo.Service");
        rule.setEnabled(false);
        when(mapper.selectOneById(7L)).thenReturn(rule);

        MockRuleEntity toggled = mockRuleService.toggleEnabled(7L);

        assertEquals(true, toggled.getEnabled());
        verify(mapper).update(rule);
        verify(sseBroadcastService).broadcastRuleChange("demo.Service", 7L, "ENABLE");
    }
}
