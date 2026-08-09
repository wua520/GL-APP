package com.fitness.server.knowledge;

import java.util.List;

public record KnowledgeMatch(String content, List<KnowledgeCitation> citations, Status status) {
    public enum Status {
        DISABLED,
        EMPTY_QUERY,
        NO_VECTOR_HIT,
        BELOW_SCORE_THRESHOLD,
        AUDIT_FILTERED,
        EMBEDDING_UNAVAILABLE,
        VECTOR_STORE_UNAVAILABLE,
        MYSQL_VERIFICATION_UNAVAILABLE,
        MATCHED
    }

    public KnowledgeMatch(String content, List<KnowledgeCitation> citations) {
        this(content, citations, content != null && !content.isBlank() && citations != null && !citations.isEmpty()
            ? Status.MATCHED : Status.NO_VECTOR_HIT);
    }

    public static KnowledgeMatch empty(Status status) {
        return new KnowledgeMatch("", List.of(), status);
    }

    public boolean hasContent() {
        return content != null && !content.isBlank() && citations != null && !citations.isEmpty();
    }
}
