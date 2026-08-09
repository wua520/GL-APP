package com.fitness.server.agent;

import com.fitness.server.entity.AgentAction;
import com.fitness.server.entity.AgentTask;
import com.fitness.server.mapper.AgentActionMapper;
import com.fitness.server.mapper.AgentTaskMapper;
import com.fitness.server.service.AgentOrchestrator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Agent 草案动作的状态机集成测试。
 *
 * 状态边界以服务端当前模型为准：
 * WAITING_CONFIRMATION → LOCAL_WRITE_PENDING → SUCCEEDED；取消为 CANCELLED。
 */
@SpringBootTest
@Transactional
class AgentStateMachineTest {

    private static final Long TEST_USER_ID = 10001L;

    @Autowired
    private AgentOrchestrator agentOrchestrator;

    @Autowired
    private AgentActionMapper agentActionMapper;

    @Autowired
    private AgentTaskMapper agentTaskMapper;

    @Test
    void confirmAndCompleteLocalWriteTransitionsActionToSucceeded() {
        TestFixture fixture = fixture("CREATE_TRAINING_PLAN");

        agentOrchestrator.confirmAction(TEST_USER_ID, fixture.taskId(), fixture.actionId());
        assertEquals("LOCAL_WRITE_PENDING", statusOf(fixture.actionId()));

        agentOrchestrator.completeLocalWrite(
            TEST_USER_ID, fixture.taskId(), fixture.actionId(), "1"
        );

        assertEquals("SUCCEEDED", statusOf(fixture.actionId()));
    }

    @Test
    void cancellingWaitingActionTransitionsOnlyThatActionToCancelled() {
        TestFixture fixture = fixture("CREATE_DIET_RECORD");

        agentOrchestrator.cancelAction(TEST_USER_ID, fixture.taskId(), fixture.actionId());

        assertEquals("CANCELLED", statusOf(fixture.actionId()));
        assertEquals("CANCELLED", agentTaskMapper.getById(fixture.taskId()).getStatus());
    }

    @Test
    void cancelledActionCannotBeConfirmed() {
        TestFixture fixture = fixture("CREATE_DIET_RECORD");
        agentOrchestrator.cancelAction(TEST_USER_ID, fixture.taskId(), fixture.actionId());

        assertThrows(IllegalStateException.class,
            () -> agentOrchestrator.confirmAction(TEST_USER_ID, fixture.taskId(), fixture.actionId()));
        assertEquals("CANCELLED", statusOf(fixture.actionId()));
    }

    @Test
    void cancelledActionCannotBeEdited() {
        TestFixture fixture = fixture("CREATE_TRAINING_PLAN");
        agentOrchestrator.cancelAction(TEST_USER_ID, fixture.taskId(), fixture.actionId());

        assertThrows(IllegalStateException.class,
            () -> agentOrchestrator.updateActionPayload(
                TEST_USER_ID, fixture.taskId(), fixture.actionId(), "{}"
            ));
        assertEquals("CANCELLED", statusOf(fixture.actionId()));
    }

    @Test
    void editingDraftRejectsPayloadThatViolatesTheCreationContract() {
        TestFixture fixture = fixture("CREATE_DIET_RECORD");
        String invalidPayload = "{\"date\":\"" + java.time.LocalDate.now() + "\",\"records\":[],\"unexpected\":true}";

        assertThrows(IllegalArgumentException.class,
            () -> agentOrchestrator.updateActionPayload(
                TEST_USER_ID, fixture.taskId(), fixture.actionId(), invalidPayload
            ));
        assertEquals("WAITING_CONFIRMATION", statusOf(fixture.actionId()));
        assertEquals("{}", agentActionMapper.getById(fixture.actionId()).getPayloadJson());
    }

    @Test
    void confirmedActionCannotBeCancelled() {
        TestFixture fixture = fixture("CREATE_TRAINING_PLAN");
        agentOrchestrator.confirmAction(TEST_USER_ID, fixture.taskId(), fixture.actionId());

        assertThrows(IllegalStateException.class,
            () -> agentOrchestrator.cancelAction(TEST_USER_ID, fixture.taskId(), fixture.actionId()));
        assertEquals("LOCAL_WRITE_PENDING", statusOf(fixture.actionId()));
    }

    @Test
    void pendingLocalWritesExposesConfirmedActionsForRecovery() {
        TestFixture fixture = fixture("CREATE_TRAINING_PLAN");
        agentOrchestrator.confirmAction(TEST_USER_ID, fixture.taskId(), fixture.actionId());

        assertEquals(1, agentOrchestrator.getPendingLocalWrites(TEST_USER_ID).stream()
            .filter(item -> fixture.actionId().equals(item.getActionId()))
            .count());
    }

    @Test
    void confirmingOneActionPreservesSiblingActionAndAggregatesTaskState() {
        TestFixture first = fixture("CREATE_TRAINING_PLAN");
        Long siblingActionId = actionFor(first.taskId(), "CREATE_DIET_RECORD", System.currentTimeMillis() + 300_000);

        agentOrchestrator.confirmAction(TEST_USER_ID, first.taskId(), first.actionId());

        assertEquals("LOCAL_WRITE_PENDING", statusOf(first.actionId()));
        assertEquals("WAITING_CONFIRMATION", statusOf(siblingActionId));
        assertEquals("LOCAL_WRITE_PENDING", agentTaskMapper.getById(first.taskId()).getStatus());
    }

    @Test
    void cancellingSiblingDoesNotCancelActionAwaitingLocalWrite() {
        TestFixture first = fixture("CREATE_TRAINING_PLAN");
        Long siblingActionId = actionFor(first.taskId(), "CREATE_DIET_RECORD", System.currentTimeMillis() + 300_000);
        agentOrchestrator.confirmAction(TEST_USER_ID, first.taskId(), first.actionId());

        agentOrchestrator.cancelAction(TEST_USER_ID, first.taskId(), siblingActionId);

        assertEquals("LOCAL_WRITE_PENDING", statusOf(first.actionId()));
        assertEquals("CANCELLED", statusOf(siblingActionId));
        assertEquals("LOCAL_WRITE_PENDING", agentTaskMapper.getById(first.taskId()).getStatus());
    }

    @Test
    void anotherUserCannotConfirmCancelOrCompleteAnOwnedAction() {
        TestFixture fixture = fixture("CREATE_TRAINING_PLAN");
        Long anotherUserId = TEST_USER_ID + 1;

        assertThrows(IllegalArgumentException.class,
            () -> agentOrchestrator.confirmAction(anotherUserId, fixture.taskId(), fixture.actionId()));
        assertThrows(IllegalArgumentException.class,
            () -> agentOrchestrator.cancelAction(anotherUserId, fixture.taskId(), fixture.actionId()));
        assertThrows(IllegalArgumentException.class,
            () -> agentOrchestrator.completeLocalWrite(
                anotherUserId, fixture.taskId(), fixture.actionId(), "foreign-plan"
            ));
        assertEquals("WAITING_CONFIRMATION", statusOf(fixture.actionId()));
    }

    @Test
    void completingLocalWriteIsIdempotentForTheSameReference() {
        TestFixture fixture = fixture("CREATE_TRAINING_PLAN");
        agentOrchestrator.confirmAction(TEST_USER_ID, fixture.taskId(), fixture.actionId());
        agentOrchestrator.completeLocalWrite(
            TEST_USER_ID, fixture.taskId(), fixture.actionId(), "1"
        );

        agentOrchestrator.completeLocalWrite(
            TEST_USER_ID, fixture.taskId(), fixture.actionId(), "1"
        );

        assertEquals("SUCCEEDED", statusOf(fixture.actionId()));
    }

    @Test
    void completingLocalWriteWithDifferentReferenceIsRejected() {
        TestFixture fixture = fixture("CREATE_TRAINING_PLAN");
        agentOrchestrator.confirmAction(TEST_USER_ID, fixture.taskId(), fixture.actionId());
        agentOrchestrator.completeLocalWrite(
            TEST_USER_ID, fixture.taskId(), fixture.actionId(), "1"
        );

        assertThrows(IllegalStateException.class,
            () -> agentOrchestrator.completeLocalWrite(
                TEST_USER_ID, fixture.taskId(), fixture.actionId(), "2"
            ));
        assertEquals("SUCCEEDED", statusOf(fixture.actionId()));
    }

    @Test
    void legacyAndTypedReferencesAreIdempotentlyEquivalent() {
        TestFixture fixture = fixture("CREATE_TRAINING_PLAN");
        agentOrchestrator.confirmAction(TEST_USER_ID, fixture.taskId(), fixture.actionId());
        agentOrchestrator.completeLocalWrite(
            TEST_USER_ID, fixture.taskId(), fixture.actionId(), "1"
        );

        agentOrchestrator.completeLocalWrite(
            TEST_USER_ID, fixture.taskId(), fixture.actionId(),
            "{\"ids\":[1],\"type\":\"training_plan\"}"
        );

        assertEquals("SUCCEEDED", statusOf(fixture.actionId()));
    }

    @Test
    void typedReferenceRejectsUnknownFieldsAndDuplicateIds() {
        TestFixture training = fixture("CREATE_TRAINING_PLAN");
        agentOrchestrator.confirmAction(TEST_USER_ID, training.taskId(), training.actionId());
        assertThrows(IllegalArgumentException.class,
            () -> agentOrchestrator.completeLocalWrite(
                TEST_USER_ID, training.taskId(), training.actionId(),
                "{\"type\":\"training_plan\",\"ids\":[1],\"extra\":true}"
            ));

        TestFixture diet = fixture("CREATE_DIET_RECORD");
        agentOrchestrator.confirmAction(TEST_USER_ID, diet.taskId(), diet.actionId());
        assertThrows(IllegalArgumentException.class,
            () -> agentOrchestrator.completeLocalWrite(
                TEST_USER_ID, diet.taskId(), diet.actionId(),
                "{\"type\":\"diet_records\",\"ids\":[1,1]}"
            ));
    }

    @Test
    void confirmingExpiredActionDoesNotAdvanceState() {
        TestFixture fixture = fixture("CREATE_DIET_RECORD", System.currentTimeMillis() - 1);

        assertThrows(IllegalArgumentException.class,
            () -> agentOrchestrator.confirmAction(TEST_USER_ID, fixture.taskId(), fixture.actionId()));
        assertEquals("EXPIRED", statusOf(fixture.actionId()));
    }

    @Test
    void confirmationAtExpiryBoundaryExpiresTheAction() {
        TestFixture fixture = fixture("CREATE_DIET_RECORD", System.currentTimeMillis());

        assertThrows(IllegalArgumentException.class,
            () -> agentOrchestrator.confirmAction(TEST_USER_ID, fixture.taskId(), fixture.actionId()));
        assertEquals("EXPIRED", statusOf(fixture.actionId()));
    }

    @Test
    void replacingOneDraftTypePreservesOtherPendingDraftTypes() {
        TestFixture first = fixture("CREATE_TRAINING_PLAN");
        Long sameTypeActionId = actionFor(first.taskId(), "CREATE_TRAINING_PLAN", System.currentTimeMillis() + 300_000);
        Long otherTypeActionId = actionFor(first.taskId(), "CREATE_DIET_RECORD", System.currentTimeMillis() + 300_000);

        agentActionMapper.replaceWaitingActionsByType(
            first.taskId(), "CREATE_TRAINING_PLAN", System.currentTimeMillis()
        );

        assertEquals("REPLACED", statusOf(first.actionId()));
        assertEquals("REPLACED", statusOf(sameTypeActionId));
        assertEquals("WAITING_CONFIRMATION", statusOf(otherTypeActionId));
    }

    @Test
    void replacingDraftDoesNotReplaceAnActionAwaitingLocalWrite() {
        TestFixture first = fixture("CREATE_TRAINING_PLAN");
        agentOrchestrator.confirmAction(TEST_USER_ID, first.taskId(), first.actionId());
        Long waitingActionId = actionFor(first.taskId(), "CREATE_TRAINING_PLAN", System.currentTimeMillis() + 300_000);

        agentActionMapper.replaceWaitingActionsByType(
            first.taskId(), "CREATE_TRAINING_PLAN", System.currentTimeMillis()
        );

        assertEquals("LOCAL_WRITE_PENDING", statusOf(first.actionId()));
        assertEquals("REPLACED", statusOf(waitingActionId));
        assertEquals("LOCAL_WRITE_PENDING", agentTaskMapper.getById(first.taskId()).getStatus());
    }

    @Test
    void cancellingTaskPreservesActionAwaitingLocalWriteAndCancelsWaitingSiblings() {
        TestFixture first = fixture("CREATE_TRAINING_PLAN");
        Long siblingActionId = actionFor(first.taskId(), "CREATE_DIET_RECORD", System.currentTimeMillis() + 300_000);
        agentOrchestrator.confirmAction(TEST_USER_ID, first.taskId(), first.actionId());

        agentOrchestrator.cancelTask(TEST_USER_ID, first.taskId());

        assertEquals("LOCAL_WRITE_PENDING", statusOf(first.actionId()));
        assertEquals("CANCELLED", statusOf(siblingActionId));
        assertEquals("LOCAL_WRITE_PENDING", agentTaskMapper.getById(first.taskId()).getStatus());
    }

    @Test
    void cancellingTaskCancelsAllWaitingActionsAndAggregatesTerminalState() {
        TestFixture first = fixture("CREATE_TRAINING_PLAN");
        Long siblingActionId = actionFor(first.taskId(), "CREATE_DIET_RECORD", System.currentTimeMillis() + 300_000);

        agentOrchestrator.cancelTask(TEST_USER_ID, first.taskId());

        assertEquals("CANCELLED", statusOf(first.actionId()));
        assertEquals("CANCELLED", statusOf(siblingActionId));
        assertEquals("CANCELLED", agentTaskMapper.getById(first.taskId()).getStatus());
    }

    @Test
    void replacedActionCannotBeConfirmed() {
        TestFixture fixture = fixture("CREATE_TRAINING_PLAN");
        agentActionMapper.replaceWaitingActionsByType(
            fixture.taskId(), "CREATE_TRAINING_PLAN", System.currentTimeMillis()
        );

        assertThrows(IllegalStateException.class,
            () -> agentOrchestrator.confirmAction(TEST_USER_ID, fixture.taskId(), fixture.actionId()));
        assertEquals("REPLACED", statusOf(fixture.actionId()));
    }

    @Test
    void replacedActionsDoNotKeepTaskInWaitingConfirmation() {
        TestFixture replaced = fixture("CREATE_TRAINING_PLAN");
        Long cancellableActionId = actionFor(
            replaced.taskId(), "CREATE_DIET_RECORD", System.currentTimeMillis() + 300_000
        );
        agentActionMapper.replaceWaitingActionsByType(
            replaced.taskId(), "CREATE_TRAINING_PLAN", System.currentTimeMillis()
        );

        agentOrchestrator.cancelAction(TEST_USER_ID, replaced.taskId(), cancellableActionId);

        assertEquals("REPLACED", statusOf(replaced.actionId()));
        assertEquals("CANCELLED", statusOf(cancellableActionId));
        assertEquals("CANCELLED", agentTaskMapper.getById(replaced.taskId()).getStatus());
    }

    private TestFixture fixture(String type) {
        return fixture(type, System.currentTimeMillis() + 300_000);
    }

    private TestFixture fixture(String type, long expiresAt) {
        AgentTask task = new AgentTask();
        task.setUserId(TEST_USER_ID);
        task.setUserContent("状态机测试");
        task.setStatus("WAITING_CONFIRMATION");
        task.setCreatedAt(System.currentTimeMillis());
        agentTaskMapper.insert(task);

        AgentAction action = new AgentAction();
        action.setTaskId(task.getId());
        action.setType(type);
        action.setPayloadJson("{}");
        action.setStatus("WAITING_CONFIRMATION");
        action.setIdempotencyKey("state-machine-" + task.getId());
        action.setExpiresAt(expiresAt);
        action.setCreatedAt(System.currentTimeMillis());
        agentActionMapper.insert(action);
        return new TestFixture(task.getId(), action.getId());
    }

    private Long actionFor(Long taskId, String type, long expiresAt) {
        AgentAction action = new AgentAction();
        action.setTaskId(taskId);
        action.setType(type);
        action.setPayloadJson("{}");
        action.setStatus("WAITING_CONFIRMATION");
        action.setIdempotencyKey("state-machine-" + taskId + "-" + type);
        action.setExpiresAt(expiresAt);
        action.setCreatedAt(System.currentTimeMillis());
        agentActionMapper.insert(action);
        return action.getId();
    }

    private String statusOf(Long actionId) {
        return agentActionMapper.getById(actionId).getStatus();
    }

    private record TestFixture(Long taskId, Long actionId) {
    }
}
