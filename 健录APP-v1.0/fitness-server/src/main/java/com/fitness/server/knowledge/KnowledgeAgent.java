package com.fitness.server.knowledge;

import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
public class KnowledgeAgent {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(KnowledgeAgent.class);
    private final KnowledgeSearch retriever;
    private final KnowledgeProperties properties;

    public KnowledgeAgent(KnowledgeSearch retriever, KnowledgeProperties properties) {
        this.retriever = retriever;
        this.properties = properties;
    }

    public String enrichGeneralAdvice(String userMessage, String originalMessage) {
        return enrichGeneralAdviceWithCitations(userMessage, originalMessage).message();
    }

    /**
     * 保留命中的来源元数据，使编排层能写入审计而无需再次检索。
     */
    public EnrichmentResult enrichGeneralAdviceWithCitations(String userMessage, String originalMessage) {
        if (!properties.isEnabled()) {
            logger.debug("Knowledge sidecar skipped: disabled");
            return EnrichmentResult.unchanged(originalMessage);
        }
        if (!isGeneralKnowledgeIntent(userMessage)) {
            logger.debug("Knowledge sidecar skipped: non-general intent");
            return EnrichmentResult.unchanged(originalMessage);
        }
        if (originalMessage == null || originalMessage.isBlank()) {
            logger.debug("Knowledge sidecar skipped: empty original response");
            return EnrichmentResult.unchanged(originalMessage);
        }
        KnowledgeMatch match = retriever.retrieve(userMessage);
        if (!match.hasContent()) {
            logger.info("Knowledge sidecar no append: status={}", match.status());
            return EnrichmentResult.unchanged(originalMessage);
        }
        String advice = "\n\n知识建议（仅供通用参考，不替代你的实时记录或专业诊断）：\n" + match.content();
        if (!properties.getRetrieval().isShowSources()) {
            return new EnrichmentResult(originalMessage + advice, match.citations());
        }
        String references = match.citations().stream()
            .map(citation -> "- " + citation.title() + "｜" + citation.sourceName() + "｜版本 " + citation.version()
                + "｜分块 " + citation.chunkId())
            .collect(Collectors.joining("\n"));
        return new EnrichmentResult(originalMessage + advice + "\n\n来源：\n" + references, match.citations());
    }

    public record EnrichmentResult(String message, java.util.List<KnowledgeCitation> citations) {
        static EnrichmentResult unchanged(String message) {
            return new EnrichmentResult(message, java.util.List.of());
        }

        public boolean hasKnowledgeHit() {
            return !citations.isEmpty();
        }
    }

    private boolean isGeneralKnowledgeIntent(String message) {
        if (message == null || message.isBlank()) return false;
        String normalized = message.toLowerCase();
        boolean asksKnowledge = containsAny(normalized, "原理", "为什么", "如何恢复", "怎么恢复", "营养常识", "训练建议", "产品帮助", "怎么用", "使用方法", "注意事项");
        boolean requestsRealtimeFact = containsAny(normalized, "今天", "昨天", "前天", "最近", "记录", "计划", "完成", "体重", "进度", "吃了什么", "练什么", "保存", "创建", "修改", "删除", "确认");
        return asksKnowledge && !requestsRealtimeFact;
    }

    private boolean containsAny(String message, String... words) {
        for (String word : words) if (message.contains(word)) return true;
        return false;
    }
}
