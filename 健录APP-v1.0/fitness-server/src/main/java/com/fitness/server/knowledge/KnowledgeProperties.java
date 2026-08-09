package com.fitness.server.knowledge;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "knowledge")
public class KnowledgeProperties {
    private boolean enabled = false;
    private boolean importSeedsOnStart = false;
    private boolean rebuildOnStart = false;
    private final Retrieval retrieval = new Retrieval();
    private final Qdrant qdrant = new Qdrant();
    private final Ollama ollama = new Ollama();

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public boolean isImportSeedsOnStart() { return importSeedsOnStart; }
    public void setImportSeedsOnStart(boolean importSeedsOnStart) { this.importSeedsOnStart = importSeedsOnStart; }
    public boolean isRebuildOnStart() { return rebuildOnStart; }
    public void setRebuildOnStart(boolean rebuildOnStart) { this.rebuildOnStart = rebuildOnStart; }
    public Retrieval getRetrieval() { return retrieval; }
    public Qdrant getQdrant() { return qdrant; }
    public Ollama getOllama() { return ollama; }

    public static class Retrieval {
        private int topK = 5;
        private double scoreThreshold = 0.72D;
        private int connectTimeoutMs = 1000;
        private int readTimeoutMs = 2000;
        private boolean showSources = true;

        public int getTopK() { return topK; }
        public void setTopK(int topK) { this.topK = topK; }
        public double getScoreThreshold() { return scoreThreshold; }
        public void setScoreThreshold(double scoreThreshold) { this.scoreThreshold = scoreThreshold; }
        public int getConnectTimeoutMs() { return connectTimeoutMs; }
        public void setConnectTimeoutMs(int connectTimeoutMs) { this.connectTimeoutMs = connectTimeoutMs; }
        public int getReadTimeoutMs() { return readTimeoutMs; }
        public void setReadTimeoutMs(int readTimeoutMs) { this.readTimeoutMs = readTimeoutMs; }
        public boolean isShowSources() { return showSources; }
        public void setShowSources(boolean showSources) { this.showSources = showSources; }
    }

    public static class Qdrant {
        private String baseUrl = "http://127.0.0.1:6333";
        private String collection = "fitness_verified_knowledge";
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public String getCollection() { return collection; }
        public void setCollection(String collection) { this.collection = collection; }
    }

    public static class Ollama {
        private String baseUrl = "http://127.0.0.1:11434";
        private String embeddingModel = "nomic-embed-text";
        private int vectorDimension = 768;
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public String getEmbeddingModel() { return embeddingModel; }
        public void setEmbeddingModel(String embeddingModel) { this.embeddingModel = embeddingModel; }
        public int getVectorDimension() { return vectorDimension; }
        public void setVectorDimension(int vectorDimension) { this.vectorDimension = vectorDimension; }
    }
}
