package com.fitness.server.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fitness.server.agent.dto.DietRecordDraftDto;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class NutritionAgentContractTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void unfamiliarConsumedFoodRequiresLlmEstimation() throws Exception {
        NutritionAgent agent = new NutritionAgent();
        Method extractor = NutritionAgent.class.getDeclaredMethod(
            "extractDietRecordArguments", String.class
        );
        extractor.setAccessible(true);

        DietRecordDraftDto draft = (DietRecordDraftDto) extractor.invoke(agent, "我吃了一锅热蚂蚁");

        assertNotNull(draft);
        assertEquals(1, draft.getRecords().size());
        assertEquals("热蚂蚁", draft.getRecords().get(0).getFoodName());
        assertEquals("一锅", draft.getRecords().get(0).getAmount());
        assertEquals(true, draft.getRecords().get(0).isEstimated());
    }

    @Test
    void familiarFoodRemainsDeterministic() throws Exception {
        NutritionAgent agent = new NutritionAgent();
        Method extractor = NutritionAgent.class.getDeclaredMethod(
            "extractDietRecordArguments", String.class
        );
        extractor.setAccessible(true);

        DietRecordDraftDto draft = (DietRecordDraftDto) extractor.invoke(agent, "我吃了100克鸡胸肉");

        assertNotNull(draft);
        assertEquals(1, draft.getRecords().size());
        assertEquals(false, draft.getRecords().get(0).isEstimated());
    }
}
