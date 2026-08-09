package com.fitness.server.knowledge;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 可独立重建的知识分块及其向量索引状态。
 */
@TableName("knowledge_chunks")
public class KnowledgeChunk {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long documentId;
    private Integer chunkIndex;
    private String chunkText;
    private String contentHash;
    private String indexStatus;
    private String qdrantPointId;
    private String embeddingModel;
    private Integer vectorDimension;
    private Long indexedAt;
    private String indexError;
    private Long createdAt;
    private Long updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getDocumentId() { return documentId; }
    public void setDocumentId(Long documentId) { this.documentId = documentId; }
    public Integer getChunkIndex() { return chunkIndex; }
    public void setChunkIndex(Integer chunkIndex) { this.chunkIndex = chunkIndex; }
    public String getChunkText() { return chunkText; }
    public void setChunkText(String chunkText) { this.chunkText = chunkText; }
    public String getContentHash() { return contentHash; }
    public void setContentHash(String contentHash) { this.contentHash = contentHash; }
    public String getIndexStatus() { return indexStatus; }
    public void setIndexStatus(String indexStatus) { this.indexStatus = indexStatus; }
    public String getQdrantPointId() { return qdrantPointId; }
    public void setQdrantPointId(String qdrantPointId) { this.qdrantPointId = qdrantPointId; }
    public String getEmbeddingModel() { return embeddingModel; }
    public void setEmbeddingModel(String embeddingModel) { this.embeddingModel = embeddingModel; }
    public Integer getVectorDimension() { return vectorDimension; }
    public void setVectorDimension(Integer vectorDimension) { this.vectorDimension = vectorDimension; }
    public Long getIndexedAt() { return indexedAt; }
    public void setIndexedAt(Long indexedAt) { this.indexedAt = indexedAt; }
    public String getIndexError() { return indexError; }
    public void setIndexError(String indexError) { this.indexError = indexError; }
    public Long getCreatedAt() { return createdAt; }
    public void setCreatedAt(Long createdAt) { this.createdAt = createdAt; }
    public Long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Long updatedAt) { this.updatedAt = updatedAt; }
}
