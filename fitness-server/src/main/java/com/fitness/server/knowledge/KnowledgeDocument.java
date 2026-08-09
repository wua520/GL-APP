package com.fitness.server.knowledge;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 审核知识文档的持久化事实，不包含用户私有健康数据。
 */
@TableName("knowledge_documents")
public class KnowledgeDocument {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String documentKey;
    private String title;
    private String category;
    private String sourceName;
    private String sourceUrl;
    private String version;
    private String reviewStatus;
    private String riskLevel;
    private Boolean allowedForAdvice;
    private Long effectiveFrom;
    private Long effectiveUntil;
    private String contentSummary;
    private String contentHash;
    private Long createdAt;
    private Long updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getDocumentKey() { return documentKey; }
    public void setDocumentKey(String documentKey) { this.documentKey = documentKey; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getSourceName() { return sourceName; }
    public void setSourceName(String sourceName) { this.sourceName = sourceName; }
    public String getSourceUrl() { return sourceUrl; }
    public void setSourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; }
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public String getReviewStatus() { return reviewStatus; }
    public void setReviewStatus(String reviewStatus) { this.reviewStatus = reviewStatus; }
    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
    public Boolean getAllowedForAdvice() { return allowedForAdvice; }
    public void setAllowedForAdvice(Boolean allowedForAdvice) { this.allowedForAdvice = allowedForAdvice; }
    public Long getEffectiveFrom() { return effectiveFrom; }
    public void setEffectiveFrom(Long effectiveFrom) { this.effectiveFrom = effectiveFrom; }
    public Long getEffectiveUntil() { return effectiveUntil; }
    public void setEffectiveUntil(Long effectiveUntil) { this.effectiveUntil = effectiveUntil; }
    public String getContentSummary() { return contentSummary; }
    public void setContentSummary(String contentSummary) { this.contentSummary = contentSummary; }
    public String getContentHash() { return contentHash; }
    public void setContentHash(String contentHash) { this.contentHash = contentHash; }
    public Long getCreatedAt() { return createdAt; }
    public void setCreatedAt(Long createdAt) { this.createdAt = createdAt; }
    public Long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Long updatedAt) { this.updatedAt = updatedAt; }
}
