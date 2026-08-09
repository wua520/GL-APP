package com.fitness.server.knowledge;

public record KnowledgeCitation(
    String title,
    String sourceName,
    String sourceUrl,
    String version,
    Long chunkId,
    double score
) { }
