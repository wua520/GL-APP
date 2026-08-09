package com.fitness.server.knowledge;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class VerifiedKnowledgeSeedImporter {
    private static final int MAX_CHUNK_CHARS = 900;
    private final KnowledgeDocumentMapper documentMapper;
    private final KnowledgeChunkMapper chunkMapper;

    public VerifiedKnowledgeSeedImporter(KnowledgeDocumentMapper documentMapper, KnowledgeChunkMapper chunkMapper) {
        this.documentMapper = documentMapper;
        this.chunkMapper = chunkMapper;
    }

    @Transactional
    public int importApprovedSeeds() {
        try {
            Resource[] resources = new PathMatchingResourcePatternResolver().getResources("classpath*:knowledge/*.yml");
            int imported = 0;
            for (Resource resource : resources) {
                if (importSeed(readSeed(resource))) imported++;
            }
            return imported;
        } catch (Exception exception) {
            throw new IllegalStateException("Verified knowledge seed import unavailable", exception);
        }
    }

    private Map<String, Object> readSeed(Resource resource) throws Exception {
        LoaderOptions options = new LoaderOptions();
        options.setAllowDuplicateKeys(false);
        Object loaded = new Yaml(new SafeConstructor(options)).load(resource.getInputStream());
        if (!(loaded instanceof Map<?, ?> rawSeed)) {
            throw new IllegalArgumentException("Knowledge seed must be a YAML object: " + resource.getFilename());
        }
        return rawSeed.entrySet().stream().collect(java.util.stream.Collectors.toMap(
            entry -> String.valueOf(entry.getKey()), Map.Entry::getValue
        ));
    }

    private boolean importSeed(Map<String, Object> seed) {
        requireApprovedSeed(seed);
        String content = required(seed, "content").trim();
        List<String> chunks = split(content);
        long now = System.currentTimeMillis();
        KnowledgeDocument document = toDocument(seed, content, now);
        KnowledgeDocument existing = documentMapper.findByKeyAndVersion(document.getDocumentKey(), document.getVersion());
        boolean changed = existing == null || !document.getContentHash().equals(existing.getContentHash());
        if (existing == null) {
            documentMapper.insert(document);
        } else {
            document.setId(existing.getId());
            documentMapper.updateSeedMetadata(document);
        }
        synchronizeChunks(document.getId(), chunks, now);
        return changed;
    }

    private void requireApprovedSeed(Map<String, Object> seed) {
        if (!"APPROVED".equals(required(seed, "reviewStatus")) || !Boolean.parseBoolean(required(seed, "allowedForAdvice"))) {
            throw new IllegalArgumentException("Only explicitly approved, advice-enabled seed documents may be imported");
        }
    }

    private KnowledgeDocument toDocument(Map<String, Object> seed, String content, long now) {
        KnowledgeDocument document = new KnowledgeDocument();
        document.setDocumentKey(required(seed, "documentKey"));
        document.setVersion(required(seed, "version"));
        document.setTitle(required(seed, "title"));
        document.setCategory(required(seed, "category"));
        document.setSourceName(required(seed, "sourceName"));
        document.setSourceUrl(required(seed, "sourceUrl"));
        document.setReviewStatus("APPROVED");
        document.setRiskLevel(required(seed, "riskLevel"));
        document.setAllowedForAdvice(true);
        document.setContentSummary(summary(content));
        document.setContentHash(hash(content));
        document.setCreatedAt(now);
        document.setUpdatedAt(now);
        return document;
    }

    private void synchronizeChunks(long documentId, List<String> texts, long now) {
        List<KnowledgeChunk> existing = chunkMapper.findByDocumentId(documentId);
        for (int index = 0; index < texts.size(); index++) {
            String text = texts.get(index);
            KnowledgeChunk chunk = new KnowledgeChunk();
            chunk.setDocumentId(documentId);
            chunk.setChunkIndex(index);
            chunk.setChunkText(text);
            chunk.setContentHash(hash(text));
            chunk.setIndexStatus("PENDING");
            chunk.setCreatedAt(now);
            chunk.setUpdatedAt(now);
            if (index < existing.size()) {
                KnowledgeChunk previous = existing.get(index);
                if (!chunk.getContentHash().equals(previous.getContentHash())) {
                    chunk.setId(previous.getId());
                    chunkMapper.replaceForReindex(chunk);
                }
            } else {
                chunkMapper.insert(chunk);
            }
        }
        chunkMapper.deleteFromIndex(documentId, texts.size());
    }

    private List<String> split(String content) {
        List<String> chunks = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String paragraph : content.split("\\R\\s*\\R")) {
            String normalized = paragraph.trim();
            if (normalized.isEmpty()) continue;
            if (current.length() > 0 && current.length() + normalized.length() + 1 > MAX_CHUNK_CHARS) {
                chunks.add(current.toString());
                current.setLength(0);
            }
            if (current.length() > 0) current.append('\n');
            current.append(normalized);
        }
        if (current.length() > 0) chunks.add(current.toString());
        if (chunks.isEmpty()) throw new IllegalArgumentException("Knowledge seed content must not be blank");
        return chunks;
    }

    private String required(Map<String, Object> seed, String field) {
        Object value = seed.get(field);
        if (value == null || String.valueOf(value).isBlank()) throw new IllegalArgumentException("Knowledge seed field is required: " + field);
        return String.valueOf(value).trim();
    }

    private String summary(String content) {
        return content.substring(0, Math.min(content.length(), 1000));
    }

    private String hash(String content) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(content.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (Exception exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }
}
