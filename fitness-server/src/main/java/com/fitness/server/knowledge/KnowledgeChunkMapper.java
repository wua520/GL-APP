package com.fitness.server.knowledge;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface KnowledgeChunkMapper {
    @Select("SELECT * FROM knowledge_chunks WHERE document_id = #{documentId} ORDER BY chunk_index")
    List<KnowledgeChunk> findByDocumentId(@Param("documentId") long documentId);

    @Insert("INSERT INTO knowledge_chunks (document_id, chunk_index, chunk_text, content_hash, index_status, " +
            "created_at, updated_at) VALUES (#{documentId}, #{chunkIndex}, #{chunkText}, #{contentHash}, " +
            "#{indexStatus}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(KnowledgeChunk chunk);

    @Update("UPDATE knowledge_chunks SET chunk_text = #{chunkText}, content_hash = #{contentHash}, index_status = 'PENDING', " +
            "qdrant_point_id = NULL, embedding_model = NULL, vector_dimension = NULL, indexed_at = NULL, index_error = NULL, " +
            "updated_at = #{updatedAt} WHERE id = #{id}")
    int replaceForReindex(KnowledgeChunk chunk);

    @Update("DELETE FROM knowledge_chunks WHERE document_id = #{documentId} AND chunk_index >= #{firstObsoleteIndex}")
    int deleteFromIndex(@Param("documentId") long documentId, @Param("firstObsoleteIndex") int firstObsoleteIndex);

    @Select({"<script>",
            "SELECT c.* FROM knowledge_chunks c ",
            "INNER JOIN knowledge_documents d ON d.id = c.document_id ",
            "WHERE d.review_status = 'APPROVED' AND d.allowed_for_advice = TRUE ",
            "AND (d.effective_from IS NULL OR d.effective_from &lt;= #{now}) ",
            "AND (d.effective_until IS NULL OR d.effective_until > #{now}) ",
            "AND c.index_status = 'INDEXED' AND c.id IN ",
            "<foreach item='chunkId' collection='chunkIds' open='(' separator=',' close=')'>",
            "#{chunkId}",
            "</foreach>",
            "</script>"})
    List<KnowledgeChunk> findAdvisableByIds(@Param("chunkIds") List<Long> chunkIds,
                                            @Param("now") long now);

    @Select("SELECT c.* FROM knowledge_chunks c INNER JOIN knowledge_documents d ON d.id = c.document_id " +
            "WHERE d.review_status = 'APPROVED' AND d.allowed_for_advice = TRUE " +
            "AND (d.effective_from IS NULL OR d.effective_from <= #{now}) " +
            "AND (d.effective_until IS NULL OR d.effective_until > #{now}) " +
            "AND c.index_status IN ('PENDING', 'FAILED', 'STALE')")
    List<KnowledgeChunk> findIndexable(@Param("now") long now);

    @Update("UPDATE knowledge_chunks c INNER JOIN knowledge_documents d ON d.id = c.document_id " +
            "SET c.index_status = 'STALE', c.index_error = NULL, c.updated_at = #{updatedAt} " +
            "WHERE d.review_status = 'APPROVED' AND d.allowed_for_advice = TRUE " +
            "AND (d.effective_from IS NULL OR d.effective_from <= #{now}) " +
            "AND (d.effective_until IS NULL OR d.effective_until > #{now})")
    int markCurrentIndexesStale(@Param("now") long now, @Param("updatedAt") long updatedAt);

    @Update("UPDATE knowledge_chunks SET index_status = #{status}, qdrant_point_id = #{pointId}, " +
            "embedding_model = #{embeddingModel}, vector_dimension = #{dimension}, indexed_at = #{indexedAt}, " +
            "index_error = #{error}, updated_at = #{indexedAt} WHERE id = #{chunkId}")
    int updateIndexStatus(@Param("chunkId") Long chunkId, @Param("status") String status,
                          @Param("pointId") String pointId, @Param("embeddingModel") String embeddingModel,
                          @Param("dimension") Integer dimension, @Param("indexedAt") Long indexedAt,
                          @Param("error") String error);
}
