package com.fitness.server.knowledge;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface KnowledgeDocumentMapper {
    @Select("SELECT * FROM knowledge_documents WHERE document_key = #{documentKey} AND version = #{version}")
    KnowledgeDocument findByKeyAndVersion(@Param("documentKey") String documentKey,
                                          @Param("version") String version);

    @Insert("INSERT INTO knowledge_documents (document_key, title, category, source_name, source_url, version, " +
            "review_status, risk_level, allowed_for_advice, effective_from, effective_until, content_summary, content_hash, " +
            "created_at, updated_at) VALUES (#{documentKey}, #{title}, #{category}, #{sourceName}, #{sourceUrl}, #{version}, " +
            "#{reviewStatus}, #{riskLevel}, #{allowedForAdvice}, #{effectiveFrom}, #{effectiveUntil}, #{contentSummary}, " +
            "#{contentHash}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(KnowledgeDocument document);

    @Update("UPDATE knowledge_documents SET title = #{title}, category = #{category}, source_name = #{sourceName}, " +
            "source_url = #{sourceUrl}, risk_level = #{riskLevel}, effective_from = #{effectiveFrom}, " +
            "effective_until = #{effectiveUntil}, content_summary = #{contentSummary}, content_hash = #{contentHash}, " +
            "updated_at = #{updatedAt} WHERE id = #{id} AND review_status = #{reviewStatus} " +
            "AND allowed_for_advice = #{allowedForAdvice}")
    int updateSeedMetadata(KnowledgeDocument document);

    @Select("SELECT * FROM knowledge_documents " +
            "WHERE review_status = 'APPROVED' AND allowed_for_advice = TRUE " +
            "AND (effective_from IS NULL OR effective_from <= #{now}) " +
            "AND (effective_until IS NULL OR effective_until > #{now})")
    List<KnowledgeDocument> findCurrentlyAdvisable(@Param("now") long now);
}
