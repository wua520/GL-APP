package com.fitness.server.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
class AgentTaskEventServiceTest {

    @Test
    void eventStreamIsDisabledByDefaultAndDoesNotSubscribe() {
        AgentTaskEventService service = new AgentTaskEventService(null, false);

        assertFalse(service.isEnabled());
        assertThrows(IllegalStateException.class, () -> service.subscribe(10001L, 846L));
    }

    @Test
    void enablingEventStreamOnlyChangesObservationChannel() {
        AgentTaskEventService service = new AgentTaskEventService(null, true);

        assertTrue(service.isEnabled());
        assertDoesNotThrow(service::isEnabled);
    }

    @Test
    void disabledEventStreamKeepsTheSynchronousContractIndependent() {
        AgentTaskEventService service = new AgentTaskEventService(null, false);

        assertFalse(service.isEnabled());
        assertThrows(IllegalStateException.class, () -> service.subscribe(10001L, 846L));
    }
}
