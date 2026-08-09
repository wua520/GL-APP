package com.fitness.server.knowledge;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KnowledgeAgentTest {

    @Test
    void disabledKnowledgeBypassesRetriever() {
        KnowledgeProperties properties = new KnowledgeProperties();
        StubSearch retriever = new StubSearch(match());

        String response = new KnowledgeAgent(retriever, properties)
            .enrichGeneralAdvice("训练原理是什么", "原始答复");

        assertEquals("原始答复", response);
        assertEquals(0, retriever.requestCount());
    }

    @Test
    void realtimeOrWriteRequestNeverReceivesKnowledgeSidecar() {
        KnowledgeProperties properties = enabledProperties();
        StubSearch retriever = new StubSearch(match());

        String response = new KnowledgeAgent(retriever, properties)
            .enrichGeneralAdvice("今天训练计划怎么修改", "原始答复");

        assertEquals("原始答复", response);
        assertEquals(0, retriever.requestCount());
    }

    @Test
    void generalKnowledgeResponseIncludesApprovedContentAndCitation() {
        KnowledgeProperties properties = enabledProperties();
        StubSearch retriever = new StubSearch(match());

        String response = new KnowledgeAgent(retriever, properties)
            .enrichGeneralAdvice("训练原理是什么", "原始答复");

        assertTrue(response.contains("渐进负荷应以可恢复为前提。"));
        assertTrue(response.contains("来源："));
        assertTrue(response.contains("基础训练原则｜健录审核知识组｜版本 2026.03-p0｜分块 7"));
        assertEquals(1, retriever.requestCount());
        assertEquals("训练原理是什么", retriever.lastQuery());
    }

    @Test
    void enrichmentResultKeepsCitationsForTaskAudit() {
        KnowledgeProperties properties = enabledProperties();

        KnowledgeAgent.EnrichmentResult result = new KnowledgeAgent(new StubSearch(match()), properties)
            .enrichGeneralAdviceWithCitations("训练原理是什么", "原始答复");

        assertTrue(result.hasKnowledgeHit());
        assertEquals(1, result.citations().size());
        assertEquals(7L, result.citations().get(0).chunkId());
    }

    @Test
    void sourceDisplayCanBeDisabledWithoutChangingAdvice() {
        KnowledgeProperties properties = enabledProperties();
        properties.getRetrieval().setShowSources(false);

        String response = new KnowledgeAgent(new StubSearch(match()), properties)
            .enrichGeneralAdvice("训练原理是什么", "原始答复");

        assertTrue(response.contains("渐进负荷应以可恢复为前提。"));
        assertTrue(!response.contains("来源："));
    }

    private KnowledgeProperties enabledProperties() {
        KnowledgeProperties properties = new KnowledgeProperties();
        properties.setEnabled(true);
        return properties;
    }

    private KnowledgeMatch match() {
        return new KnowledgeMatch(
            "渐进负荷应以可恢复为前提。",
            List.of(new KnowledgeCitation(
                "基础训练原则", "健录审核知识组", "https://example.invalid/source", "2026.03-p0", 7L, 0.9D
            ))
        );
    }

    private static final class StubSearch implements KnowledgeSearch {
        private final KnowledgeMatch match;
        private final AtomicInteger requestCount = new AtomicInteger();
        private String lastQuery;

        private StubSearch(KnowledgeMatch match) {
            this.match = match;
        }

        @Override
        public KnowledgeMatch retrieve(String query) {
            lastQuery = query;
            requestCount.incrementAndGet();
            return match;
        }

        private int requestCount() {
            return requestCount.get();
        }

        private String lastQuery() {
            return lastQuery;
        }
    }
}
