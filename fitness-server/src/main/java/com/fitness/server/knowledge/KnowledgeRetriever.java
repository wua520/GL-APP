package com.fitness.server.knowledge;

import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class KnowledgeRetriever implements KnowledgeSearch {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(KnowledgeRetriever.class);
    private final KnowledgeProperties properties;
    private final EmbeddingClient embeddingClient;
    private final VectorStore vectorStore;
    private final KnowledgeChunkMapper chunkMapper;
    private final KnowledgeDocumentMapper documentMapper;

    public KnowledgeRetriever(KnowledgeProperties properties, EmbeddingClient embeddingClient, VectorStore vectorStore,
                              KnowledgeChunkMapper chunkMapper, KnowledgeDocumentMapper documentMapper) {
        this.properties = properties;
        this.embeddingClient = embeddingClient;
        this.vectorStore = vectorStore;
        this.chunkMapper = chunkMapper;
        this.documentMapper = documentMapper;
    }

    @Override
    public KnowledgeMatch retrieve(String query) {
        if (!properties.isEnabled()) return empty(KnowledgeMatch.Status.DISABLED);
        if (query == null || query.isBlank()) return empty(KnowledgeMatch.Status.EMPTY_QUERY);

        final List<Double> embedding;
        try {
            embedding = embeddingClient.embed(query);
        } catch (Exception exception) {
            logger.warn("Knowledge retrieval embedding unavailable: {}", conciseError(exception));
            return empty(KnowledgeMatch.Status.EMBEDDING_UNAVAILABLE);
        }

        final List<VectorStore.VectorHit> rawHits;
        try {
            rawHits = vectorStore.search(embedding, properties.getRetrieval().getTopK());
        } catch (Exception exception) {
            logger.warn("Knowledge retrieval vector store unavailable: {}", conciseError(exception));
            return empty(KnowledgeMatch.Status.VECTOR_STORE_UNAVAILABLE);
        }
        if (rawHits.isEmpty()) return empty(KnowledgeMatch.Status.NO_VECTOR_HIT);

        List<VectorStore.VectorHit> vectorHits = rawHits.stream()
            .filter(hit -> hit.score() >= properties.getRetrieval().getScoreThreshold())
            .toList();
        if (vectorHits.isEmpty()) {
            logger.info("Knowledge retrieval below threshold: bestScore={}, threshold={}",
                rawHits.stream().mapToDouble(VectorStore.VectorHit::score).max().orElse(0D),
                properties.getRetrieval().getScoreThreshold());
            return empty(KnowledgeMatch.Status.BELOW_SCORE_THRESHOLD);
        }

        try {
            Map<Long, Double> scores = vectorHits.stream().collect(Collectors.toMap(
                VectorStore.VectorHit::chunkId, VectorStore.VectorHit::score, Math::max, LinkedHashMap::new
            ));
            Map<Long, KnowledgeChunk> verified = chunkMapper.findAdvisableByIds(List.copyOf(scores.keySet()), System.currentTimeMillis())
                .stream().collect(Collectors.toMap(KnowledgeChunk::getId, chunk -> chunk));
            Map<Long, KnowledgeDocument> documents = documentMapper.findCurrentlyAdvisable(System.currentTimeMillis())
                .stream().collect(Collectors.toMap(KnowledgeDocument::getId, document -> document));
            List<KnowledgeChunk> chunks = verified.values().stream()
                .filter(chunk -> documents.containsKey(chunk.getDocumentId()))
                .sorted(Comparator.comparingDouble(chunk -> -scores.get(chunk.getId())))
                .toList();
            if (chunks.isEmpty()) {
                logger.info("Knowledge retrieval audit filtered {} vector hit(s)", vectorHits.size());
                return empty(KnowledgeMatch.Status.AUDIT_FILTERED);
            }
            List<KnowledgeCitation> citations = chunks.stream().map(chunk -> {
                KnowledgeDocument document = documents.get(chunk.getDocumentId());
                return new KnowledgeCitation(document.getTitle(), document.getSourceName(), document.getSourceUrl(),
                    document.getVersion(), chunk.getId(), scores.get(chunk.getId()));
            }).toList();
            logger.info("Knowledge retrieval matched {} approved chunk(s)", citations.size());
            return new KnowledgeMatch(chunks.stream().map(KnowledgeChunk::getChunkText).collect(Collectors.joining("\n")),
                citations, KnowledgeMatch.Status.MATCHED);
        } catch (Exception exception) {
            logger.warn("Knowledge retrieval MySQL verification unavailable: {}", conciseError(exception));
            return empty(KnowledgeMatch.Status.MYSQL_VERIFICATION_UNAVAILABLE);
        }
    }

    public int rebuildAllIndexes() {
        if (!properties.isEnabled()) return 0;
        long now = System.currentTimeMillis();
        vectorStore.recreateCollection(properties.getOllama().getVectorDimension());
        chunkMapper.markCurrentIndexesStale(now, now);
        return rebuildPendingIndex();
    }

    public int rebuildPendingIndex() {
        if (!properties.isEnabled()) return 0;
        int indexed = 0;
        for (KnowledgeChunk chunk : chunkMapper.findIndexable(System.currentTimeMillis())) {
            long now = System.currentTimeMillis();
            long pointId = chunk.getId();
            try {
                List<Double> vector = embeddingClient.embed(chunk.getChunkText());
                vectorStore.upsert(pointId, chunk.getId(), vector);
                chunkMapper.updateIndexStatus(chunk.getId(), "INDEXED", Long.toString(pointId), properties.getOllama().getEmbeddingModel(),
                    vector.size(), now, null);
                indexed++;
            } catch (Exception exception) {
                chunkMapper.updateIndexStatus(chunk.getId(), "FAILED", Long.toString(pointId), properties.getOllama().getEmbeddingModel(),
                    null, now, conciseError(exception));
                logger.warn("Knowledge chunk {} indexing failed", chunk.getId());
            }
        }
        return indexed;
    }

    private String conciseError(Exception exception) {
        String message = exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage();
        return message.substring(0, Math.min(message.length(), 1000));
    }

    private KnowledgeMatch empty(KnowledgeMatch.Status status) {
        return KnowledgeMatch.empty(status);
    }
}
