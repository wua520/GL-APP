package com.fitness.server.agent;

import com.fitness.server.agent.dto.AgentResultDto;
import com.fitness.server.agent.dto.ExecutionPlan;
import com.fitness.server.agent.dto.SafetyDecision;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class F1aContractSafetyTest {

    private final SafetyPolicy safetyPolicy = new SafetyPolicy();

    @Test
    void highRiskTrainingInputBlocksDraftGeneration() {
        SafetyDecision decision = safetyPolicy.preCheck("我膝盖疼痛，帮我制定一份力量训练计划");

        assertTrue(decision.isBlockDraft());
        assertTrue(decision.getRiskLevel() == SafetyDecision.RiskLevel.BLOCK);
        assertDoesNotThrow(decision::validate);
    }

    @Test
    void normalTrainingInputKeepsDraftEligibility() {
        SafetyDecision decision = safetyPolicy.preCheck("帮我制定每周四天的增肌训练计划");

        assertFalse(decision.isBlockDraft());
        assertTrue(decision.getRiskLevel() == SafetyDecision.RiskLevel.SAFE);
        assertDoesNotThrow(decision::validate);
    }

    @Test
    void multiDomainPlanAcceptsSeparateTrainingAndNutritionDelegation() {
        ExecutionPlan plan = validTrainingPlan();
        plan.setDomains(List.of("TRAINING", "NUTRITION"));
        plan.setExecutionMode(ExecutionPlan.ExecutionMode.SEQUENTIAL);
        plan.setSelectedAgents(List.of("TrainingAgent", "NutritionAgent"));
        plan.setAllowedToolsByAgent(Map.of(
            "TrainingAgent", List.of("get_training_summary"),
            "NutritionAgent", List.of("get_diet_summary", "create_diet_record_draft")
        ));
        plan.setDraftAllowed(true);

        assertDoesNotThrow(plan::validate);
    }

    @Test
    void multiDomainPlanAcceptsReadOnlyParallelDelegation() {
        ExecutionPlan plan = validTrainingPlan();
        plan.setDomains(List.of("TRAINING", "PROGRESS"));
        plan.setExecutionMode(ExecutionPlan.ExecutionMode.PARALLEL);
        plan.setSelectedAgents(List.of("TrainingAgent", "ProgressAgent"));
        plan.setAllowedToolsByAgent(Map.of(
            "TrainingAgent", List.of("get_training_summary"),
            "ProgressAgent", List.of("get_training_progress")
        ));

        assertDoesNotThrow(plan::validate);
    }

    @Test
    void multiDomainPlanRejectsParallelDraftDelegation() {
        ExecutionPlan plan = validTrainingPlan();
        plan.setDomains(List.of("TRAINING", "NUTRITION"));
        plan.setExecutionMode(ExecutionPlan.ExecutionMode.PARALLEL);
        plan.setSelectedAgents(List.of("TrainingAgent", "NutritionAgent"));
        plan.setAllowedToolsByAgent(Map.of(
            "TrainingAgent", List.of("get_training_summary"),
            "NutritionAgent", List.of("get_diet_summary", "create_diet_record_draft")
        ));
        plan.setDraftAllowed(true);

        assertThrows(IllegalArgumentException.class, plan::validate);
    }

    @Test
    void planRejectsUnknownDomainDelegation() {
        ExecutionPlan plan = validTrainingPlan();
        plan.setDomains(List.of("KNOWLEDGE"));
        plan.setSelectedAgents(List.of("NutritionAgent"));
        plan.setAllowedToolsByAgent(Map.of("NutritionAgent", List.of("get_diet_summary")));

        assertThrows(IllegalArgumentException.class, plan::validate);
    }

    @Test
    void trainingPlanRejectsMissingTrainingToolGrant() {
        ExecutionPlan plan = validTrainingPlan();
        plan.setAllowedToolsByAgent(Map.of());

        assertThrows(IllegalArgumentException.class, plan::validate);
    }

    @Test
    void trainingPlanRejectsDraftPermissionMismatch() {
        ExecutionPlan plan = validTrainingPlan();
        plan.setDraftAllowed(true);

        assertThrows(IllegalArgumentException.class, plan::validate);
    }

    @Test
    void trainingPlanAcceptsConsistentDraftPermission() {
        ExecutionPlan plan = validTrainingPlan();
        plan.setAllowedToolsByAgent(Map.of("TrainingAgent", List.of(
            "get_training_summary", "create_training_plan_draft"
        )));
        plan.setDraftAllowed(true);

        assertDoesNotThrow(plan::validate);
    }

    @Test
    void trainingResultRejectsOutOfRangeConfidence() {
        AgentResultDto result = validTrainingResult();
        result.setConfidence(1.01);

        assertThrows(IllegalArgumentException.class, result::validateForTrainingTask);
    }

    @Test
    void blockedSafetyDecisionCannotAllowDraft() {
        SafetyDecision decision = new SafetyDecision();
        decision.setRiskLevel(SafetyDecision.RiskLevel.BLOCK);
        decision.setBlockDraft(false);
        decision.setRestrictions(new ArrayList<>());
        decision.setForbiddenActions(new ArrayList<>());

        assertThrows(IllegalArgumentException.class, decision::validate);
    }

    private ExecutionPlan validTrainingPlan() {
        ExecutionPlan plan = new ExecutionPlan();
        plan.setTaskId("task-1");
        plan.setUserId(1L);
        plan.setDomains(List.of("TRAINING"));
        plan.setExecutionMode(ExecutionPlan.ExecutionMode.SINGLE);
        plan.setSelectedAgents(List.of("TrainingAgent"));
        plan.setAllowedToolsByAgent(Map.of("TrainingAgent", List.of("get_training_summary")));
        plan.setRiskLevel(ExecutionPlan.RiskLevel.NORMAL);
        plan.setDeadlineMs(System.currentTimeMillis() + 10_000);
        plan.setRoutingReason("F1-A训练垂直切片");
        return plan;
    }

    private AgentResultDto validTrainingResult() {
        AgentResultDto result = new AgentResultDto();
        result.setTaskId("task-1");
        result.setAgentName("TrainingAgent");
        result.setStatus(AgentResultDto.ResultStatus.SUCCESS);
        result.setFacts(new ArrayList<>());
        result.setFindings(new ArrayList<>());
        result.setConstraints(new ArrayList<>());
        result.setEvidenceRefs(new ArrayList<>());
        result.setConfidence(0.8);
        return result;
    }
}
