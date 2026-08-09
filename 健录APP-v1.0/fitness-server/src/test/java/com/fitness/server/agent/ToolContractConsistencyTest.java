package com.fitness.server.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ToolContractConsistencyTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void recentWorkoutsUsesRangeDaysAcrossThePublishedContractAndRuntimeValidator() throws Exception {
        ToolContractRegistry registry = new ToolContractRegistry();
        Map<String, Object> schema = registry.getContract("get_recent_workouts").getInputSchema();
        Map<String, Object> properties = propertiesOf(schema);

        assertTrue(properties.containsKey("rangeDays"));
        assertFalse(properties.containsKey("limit"));
        assertFalse(properties.containsKey("days"));
        assertEquals(7, registry.getContract("get_recent_workouts")
            .validateAndNormalize("{\"rangeDays\":7}").get("rangeDays"));
    }

    @Test
    void contractRejectsUnknownFieldsMalformedTypesAndInvalidDates() throws Exception {
        assertInvalid("get_recent_workouts", "[]");
        assertInvalid("get_recent_workouts", "{\"rangeDays\":\"7\"}");
        assertInvalid("get_recent_workouts", "{\"rangeDays\":7,\"days\":7}");
        assertInvalid("get_training_schedule", "{\"date\":\"not-a-date\"}");
        assertInvalid("get_training_progress", "{\"rangeDays\":6}");
    }

    @Test
    void contractEnforcesTrainingDraftBoundaryConstraints() throws Exception {
        assertInvalid("create_training_plan_draft", """
            {"title":"计划","goal":"增肌","trainingDays":1,"days":[
              {"name":"第1天","focus":"胸","exercises":[
                {"name":"卧推","sets":3,"reps":"12","restTime":"90"}
              ]}
            ]}
            """);
        assertInvalid("create_training_plan_draft", """
            {"title":"计划","goal":"增肌","trainingDays":1,"days":[
              {"name":"第1天","focus":"胸","exercises":[
                {"name":"卧推","sets":3,"reps":"12次","restTime":"90秒","notes":"这是一段超过十五个字符的动作说明"},
                {"name":"飞鸟","sets":3,"reps":"12次","restTime":"90秒"},
                {"name":"夹胸","sets":3,"reps":"12次","restTime":"90秒"},
                {"name":"俯卧撑","sets":3,"reps":"12次","restTime":"90秒"},
                {"name":"双杠臂屈伸","sets":3,"reps":"12次","restTime":"90秒"},
                {"name":"拉伸","sets":3,"reps":"12次","restTime":"90秒"}
              ]}
            ]}
            """);
    }

    @Test
    void contractRejectsDraftTypeCoercionNullsAndUnknownNestedFields() throws Exception {
        assertInvalid("create_training_plan_draft", """
            {"title":"计划","goal":"增肌","experience":"新手","trainingDays":"1","days":[]}
            """);
        assertInvalid("create_training_plan_draft", """
            {"title":"计划","goal":"增肌","experience":"新手","trainingDays":1,"days":[
              {"name":"第1天","focus":"胸","exercises":[
                {"name":"卧推","sets":"3","reps":"12次","restTime":"90秒","extra":true}
              ]}
            ]}
            """);
        assertInvalid("create_training_plan_draft", """
            {"title":"计划","goal":"增肌","experience":null,"trainingDays":1,"days":[
              {"name":"第1天","focus":"胸","exercises":[{"name":"卧推","sets":3,"reps":"12次","restTime":"90秒"}]}
            ]}
            """);
        assertInvalid("create_diet_record_draft", """
            {"date":"2026-01-01","records":[
              {"meal_type":"早餐","food_name":"燕麦","calories":"150","protein":"5","carbs":27,"fat":2.5,"amount":"1碗"}
            ]}
            """);
        assertInvalid("create_diet_record_draft", """
            {"date":"2026-01-01","records":[
              {"meal_type":"早餐","food_name":"燕麦","calories":150,"protein":5,"carbs":27,"fat":2.5,"amount":"1碗","extra":true}
            ]}
            """);
        assertInvalid("create_diet_record_draft", """
            {"date":"2026-01-01","records":[
              {"meal_type":"早餐","food_name":"热蚂蚁","calories":0,"protein":0,"carbs":0,"fat":0,"amount":"一锅","is_estimated":true}
            ]}
            """);
    }

    @Test
    void validationReportsTheExactUnknownParameterPath() {
        ToolContract.ValidationException exception = assertThrows(
            ToolContract.ValidationException.class,
            () -> registry().getContract("get_recent_workouts")
                .validateAndNormalize("{\"days\":7}")
        );

        assertEquals("arguments.days", exception.getField());
        assertEquals("不支持的参数", exception.getReason());
    }

    @Test
    void validationReportsTheExactUnknownDateParameterPath() {
        ToolContract.ValidationException exception = assertThrows(
            ToolContract.ValidationException.class,
            () -> registry().getContract("get_training_schedule")
                .validateAndNormalize("{\"unexpected\":true}")
        );

        assertEquals("arguments.unexpected", exception.getField());
        assertEquals("不支持的参数", exception.getReason());
    }

    @Test
    void validDraftProducesNormalizedRawArguments() throws Exception {
        Map<String, Object> normalized = registry().getContract("create_diet_record_draft")
            .validateAndNormalize("""
                {"date":"2026-01-01","records":[
                  {"meal_type":"早餐","food_name":"燕麦","calories":150,"protein":5,"carbs":27,"fat":2.5,"amount":"1碗"}
                ]}
                """);

        JsonNode raw = objectMapper.readTree((String) normalized.get("_raw"));
        assertTrue(raw.get("records").get(0).get("calories").isIntegralNumber());
        assertTrue(raw.get("records").get(0).get("protein").isNumber());
    }

    @Test
    void toolResultErrorEscapesStructuredMessages() throws Exception {
        JsonNode result = objectMapper.readTree(
            ToolResultJson.error("tool_execution_failed", "包含引号\"、反斜杠\\和换行\n", true)
        );

        assertEquals("tool_execution_failed", result.get("error").asText());
        assertTrue(result.get("retryable").asBoolean());
    }

    @Test
    void protocolResponsePreservesFinishReasonAndRejectsIncompleteToolCalls() throws Exception {
        LlmClient client = new LlmClient();
        java.lang.reflect.Method parseResponse = LlmClient.class.getDeclaredMethod("parseResponse", String.class);
        parseResponse.setAccessible(true);

        LlmClient.LlmResponse response = (LlmClient.LlmResponse) parseResponse.invoke(client, """
            {"choices":[{"finish_reason":"tool_calls","message":{"content":null,"tool_calls":[
              {"id":"call-1","type":"function","function":{"name":"get_recovery_status","arguments":"{}"}}
            ]}}]}
            """);

        assertEquals("tool_calls", response.getFinishReason());
        assertEquals("call-1", response.getToolCalls().get(0).getId());
        java.lang.reflect.InvocationTargetException exception = assertThrows(
            java.lang.reflect.InvocationTargetException.class,
            () -> parseResponse.invoke(client, """
                {"choices":[{"finish_reason":"tool_calls","message":{"tool_calls":[
                  {"type":"function","function":{"name":"get_recovery_status","arguments":"{}"}}
                ]}}]}
                """)
        );
        assertTrue(exception.getCause().getMessage().contains("无效的工具调用"));
        java.lang.reflect.InvocationTargetException duplicateIdException = assertThrows(
            java.lang.reflect.InvocationTargetException.class,
            () -> parseResponse.invoke(client, """
                {"choices":[{"finish_reason":"tool_calls","message":{"tool_calls":[
                  {"id":"call-1","type":"function","function":{"name":"get_recovery_status","arguments":"{}"}},
                  {"id":"call-1","type":"function","function":{"name":"get_recovery_status","arguments":"{}"}}
                ]}}]}
                """)
        );
        assertTrue(duplicateIdException.getCause().getMessage().contains("重复的工具调用ID"));
        java.lang.reflect.InvocationTargetException invalidArgumentsException = assertThrows(
            java.lang.reflect.InvocationTargetException.class,
            () -> parseResponse.invoke(client, """
                {"choices":[{"finish_reason":"tool_calls","message":{"tool_calls":[
                  {"id":"call-2","type":"function","function":{"name":"get_recovery_status","arguments":"[]"}}
                ]}}]}
                """)
        );
        assertTrue(invalidArgumentsException.getCause().getMessage().contains("合法JSON对象参数"));
    }

    @Test
    void toolExecutionExceptionPreservesArgumentFailureContract() {
        AgentToolExecutorV2.ToolExecutionException exception =
            AgentToolExecutorV2.ToolExecutionException.invalidArguments("rangeDays 必须是整数");

        assertEquals("invalid_arguments", exception.getCode());
        assertFalse(exception.isRetryable());
    }

    @Test
    void validatorAppliesDefaultsOnlyWhenFieldsAreAbsent() throws Exception {
        assertEquals(7, registry().getContract("get_recent_workouts")
            .validateAndNormalize("{}").get("rangeDays"));
    }

    @Test
    void noParameterToolsRejectMalformedOrUnexpectedArguments() throws Exception {
        assertInvalid("get_recovery_status", "[]");
        assertInvalid("get_recovery_status", "{\"unexpected\":true}");
        assertInvalid("get_recovery_status", "not-json");
        assertEquals(Map.of(), registry().getContract("get_recovery_status").validateAndNormalize("{}"));
    }

    @Test
    void publishedSchemasRejectUnknownFieldsAtEveryObjectBoundary() {
        Map<String, Object> schema = registry().getContract("create_training_plan_draft").getInputSchema();
        Map<String, Object> topLevelProperties = propertiesOf(schema);
        Map<String, Object> daySchema = schemaOf(topLevelProperties, "days", "items");
        Map<String, Object> exerciseSchema = schemaOf(propertiesOf(daySchema), "exercises", "items");

        assertFalse(allowsAdditionalProperties(schema));
        assertFalse(allowsAdditionalProperties(daySchema));
        assertFalse(allowsAdditionalProperties(exerciseSchema));
        assertFalse(allowsAdditionalProperties(schemaOf(
            propertiesOf(registry().getContract("create_diet_record_draft").getInputSchema()), "records", "items"
        )));
    }

    @Test
    void authorizedToolResolverReturnsOnlyPublishedToolsAndRejectsUnknownNames() {
        ToolContractRegistry registry = registry();
        AuthorizedToolResolver resolver = new AuthorizedToolResolver(registry);

        List<LlmClient.Tool> tools = resolver.resolve(List.of(
            "get_training_summary", "get_recent_workouts"
        ));

        assertEquals(List.of("get_training_summary", "get_recent_workouts"),
            tools.stream().map(LlmClient.Tool::getName).toList());
        assertEquals("training", registry.getContract("get_recovery_status").getDomainOwner());
        assertThrows(IllegalArgumentException.class,
            () -> resolver.resolve(List.of("missing_tool")));
    }

    @Test
    void authorizationRejectsUnknownCrossDomainAndDraftToolsOutsideAllowedScope() {
        AuthorizedToolResolver resolver = new AuthorizedToolResolver(registry());

        assertThrows(IllegalArgumentException.class,
            () -> resolver.validateAuthorization(List.of("missing_tool"), List.of("training"), true));
        assertThrows(IllegalArgumentException.class,
            () -> resolver.validateAuthorization(List.of("get_diet_summary"), List.of("training"), true));
        assertThrows(IllegalArgumentException.class,
            () -> resolver.validateAuthorization(List.of("create_training_plan_draft"),
                List.of("training", "nutrition", "progress"), false));
    }

    @Test
    void authorizationAllowsProgressToReadCrossDomainFactsWithoutDraftCapability() {
        AuthorizedToolResolver resolver = new AuthorizedToolResolver(registry());

        resolver.validateAuthorization(
            List.of("get_body_trend", "get_training_progress", "get_diet_summary"),
            List.of("progress", "training", "nutrition"),
            false
        );
    }

    @Test
    void registryPreservesPublishedToolOrder() {
        List<String> firstRead = registry().getAllContracts().stream().map(ToolContract::getName).toList();
        List<String> secondRead = registry().getAllContracts().stream().map(ToolContract::getName).toList();

        assertEquals(firstRead, secondRead);
        assertEquals("get_training_summary", firstRead.get(0));
        assertEquals("get_body_trend", firstRead.get(firstRead.size() - 1));
    }

    @Test
    void everyPublishedContractHasOneReachableExecutorBinding() {
        ToolContractRegistry registry = registry();
        AgentToolExecutorV2 executor = new AgentToolExecutorV2();

        registry.validateExecutorBindings(executor.getExecutorBindings());
        assertEquals(
            registry.getAllContracts().stream()
                .map(ToolContract::getExecutorBinding)
                .collect(java.util.stream.Collectors.toSet()),
            executor.getExecutorBindings()
        );
    }

    @Test
    void registryRejectsMissingExecutorBindings() {
        ToolContractRegistry registry = registry();
        Set<ToolContract.ExecutorBinding> declaredBindings = registry.getAllContracts().stream()
            .map(ToolContract::getExecutorBinding)
            .collect(java.util.stream.Collectors.toSet());

        Set<ToolContract.ExecutorBinding> missingHandler = java.util.EnumSet.copyOf(declaredBindings);
        missingHandler.remove(ToolContract.ExecutorBinding.BODY_TREND);
        assertThrows(IllegalStateException.class, () -> registry.validateExecutorBindings(missingHandler));
    }

    private ToolContractRegistry registry() {
        return new ToolContractRegistry();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> propertiesOf(Map<String, Object> schema) {
        return (Map<String, Object>) schema.get("properties");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> schemaOf(Map<String, Object> properties, String propertyName, String nestedKey) {
        return (Map<String, Object>) ((Map<String, Object>) properties.get(propertyName)).get(nestedKey);
    }

    private boolean allowsAdditionalProperties(Map<String, Object> schema) {
        return Boolean.TRUE.equals(schema.get("additionalProperties"));
    }

    private void assertInvalid(String toolName, String arguments) throws Exception {
        assertThrows(ToolContract.ValidationException.class,
            () -> registry().getContract(toolName).validateAndNormalize(arguments));
    }
}
