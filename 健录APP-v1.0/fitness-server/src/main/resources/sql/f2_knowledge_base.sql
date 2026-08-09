-- ============================================================================
-- F2 审核知识库事实模型
-- ============================================================================
-- MySQL 是审核、来源、版本和生效状态的唯一事实源。
-- Qdrant 只保存由 knowledge_chunks 重建出的向量索引，不保存审核事实。

CREATE TABLE IF NOT EXISTS knowledge_documents (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '知识文档ID',
    document_key VARCHAR(150) NOT NULL COMMENT '来源与文档稳定标识',
    title VARCHAR(255) NOT NULL COMMENT '文档标题',
    category VARCHAR(50) NOT NULL COMMENT 'TRAINING/NUTRITION/RECOVERY/HELP',
    source_name VARCHAR(255) NOT NULL COMMENT '来源名称',
    source_url VARCHAR(1000) COMMENT '来源地址',
    version VARCHAR(80) NOT NULL COMMENT '来源版本或审核版本',
    review_status VARCHAR(30) NOT NULL COMMENT 'DRAFT/APPROVED/REVOKED/ARCHIVED',
    risk_level VARCHAR(20) NOT NULL COMMENT 'P0/P1/P2',
    allowed_for_advice BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否允许用于通用建议',
    effective_from BIGINT COMMENT '生效时间（毫秒）',
    effective_until BIGINT COMMENT '失效时间（毫秒）',
    content_summary VARCHAR(1000) COMMENT '不含用户数据的内容摘要',
    content_hash CHAR(64) NOT NULL COMMENT '原始文档内容哈希',
    created_at BIGINT NOT NULL COMMENT '创建时间（毫秒）',
    updated_at BIGINT NOT NULL COMMENT '更新时间（毫秒）',
    UNIQUE KEY uk_knowledge_document_version (document_key, version),
    INDEX idx_knowledge_document_status (review_status, allowed_for_advice),
    INDEX idx_knowledge_document_effective (effective_from, effective_until)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审核知识文档事实表';

CREATE TABLE IF NOT EXISTS knowledge_chunks (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '知识分块ID',
    document_id BIGINT NOT NULL COMMENT '所属文档ID',
    chunk_index INT NOT NULL COMMENT '文档内分块顺序',
    chunk_text TEXT NOT NULL COMMENT '经审核的分块正文',
    content_hash CHAR(64) NOT NULL COMMENT '分块正文哈希',
    index_status VARCHAR(30) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/INDEXED/FAILED/STALE',
    qdrant_point_id VARCHAR(150) COMMENT 'Qdrant point ID，可由本表重建',
    embedding_model VARCHAR(150) COMMENT '生成向量的模型',
    vector_dimension INT COMMENT '向量维度',
    indexed_at BIGINT COMMENT '向量索引时间（毫秒）',
    index_error VARCHAR(1000) COMMENT '最近一次索引错误摘要',
    created_at BIGINT NOT NULL COMMENT '创建时间（毫秒）',
    updated_at BIGINT NOT NULL COMMENT '更新时间（毫秒）',
    UNIQUE KEY uk_knowledge_chunk_order (document_id, chunk_index),
    INDEX idx_knowledge_chunk_document (document_id),
    INDEX idx_knowledge_chunk_index_status (index_status),
    CONSTRAINT fk_knowledge_chunk_document
        FOREIGN KEY (document_id) REFERENCES knowledge_documents(id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审核知识分块及索引状态表';

-- 导入器必须按 document_key + version 幂等，不得以标题覆盖其他版本。
-- 检索时必须联结两表，并同时满足 APPROVED、allowed_for_advice、有效期和 INDEXED。
