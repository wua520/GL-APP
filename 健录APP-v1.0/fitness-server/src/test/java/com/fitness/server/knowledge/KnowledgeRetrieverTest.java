package com.fitness.server.knowledge;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KnowledgeRetrieverTest {

    @Test
    void disabledKnowledgeNeverCallsLocalServices() {
        KnowledgeProperties properties = new KnowledgeProperties();
        EmbeddingClient embeddingClient = mock(EmbeddingClient.class);
        VectorStore vectorStore = mock(VectorStore.class);
        KnowledgeChunkMapper chunkMapper = mock(KnowledgeChunkMapper.class);
        KnowledgeDocumentMapper documentMapper = mock(KnowledgeDocumentMapper.class);

        KnowledgeMatch match = new KnowledgeRetriever(
            properties, embeddingClient, vectorStore, chunkMapper, documentMapper
        ).retrieve("训练原则");

        assertTrue(match.content().isEmpty());
        assertTrue(match.citations().isEmpty());
    }

    @Test
    void retrievalOnlyReturnsChunksThatPassMysqlVerification() {
        KnowledgeProperties properties = new KnowledgeProperties();
        properties.setEnabled(true);
        EmbeddingClient embeddingClient = mock(EmbeddingClient.class);
        VectorStore vectorStore = mock(VectorStore.class);
        KnowledgeChunkMapper chunkMapper = mock(KnowledgeChunkMapper.class);
        KnowledgeDocumentMapper documentMapper = mock(KnowledgeDocumentMapper.class);
        KnowledgeChunk verifiedChunk = chunk(7L, 3L, "渐进负荷应以可恢复为前提。");
        KnowledgeDocument approvedDocument = document(3L);

        when(embeddingClient.embed("训练原则")).thenReturn(List.of(0.1D, 0.2D));
        when(vectorStore.search(List.of(0.1D, 0.2D), properties.getRetrieval().getTopK()))
            .thenReturn(List.of(new VectorStore.VectorHit(7L, 0.9D), new VectorStore.VectorHit(8L, 0.95D)));
        when(chunkMapper.findAdvisableByIds(eq(List.of(7L, 8L)), anyLong())).thenReturn(List.of(verifiedChunk));
        when(documentMapper.findCurrentlyAdvisable(anyLong())).thenReturn(List.of(approvedDocument));

        KnowledgeMatch match = new KnowledgeRetriever(
            properties, embeddingClient, vectorStore, chunkMapper, documentMapper
        ).retrieve("训练原则");

        assertEquals("渐进负荷应以可恢复为前提。", match.content());
        assertEquals(1, match.citations().size());
        assertEquals(7L, match.citations().get(0).chunkId());
    }

    @Test
    void retrievalAllowsSemanticallyRelevantScoreAboveConfiguredThreshold() {
        KnowledgeProperties properties = new KnowledgeProperties();
        properties.setEnabled(true);
        properties.getRetrieval().setScoreThreshold(0.65D);
        EmbeddingClient embeddingClient = mock(EmbeddingClient.class);
        VectorStore vectorStore = mock(VectorStore.class);
        KnowledgeChunkMapper chunkMapper = mock(KnowledgeChunkMapper.class);
        KnowledgeDocumentMapper documentMapper = mock(KnowledgeDocumentMapper.class);
        KnowledgeChunk verifiedChunk = chunk(7L, 3L, "渐进负荷应以可恢复为前提。");

        when(embeddingClient.embed("渐进负荷原理是什么")).thenReturn(List.of(0.1D, 0.2D));
        when(vectorStore.search(List.of(0.1D, 0.2D), properties.getRetrieval().getTopK()))
            .thenReturn(List.of(new VectorStore.VectorHit(7L, 0.6672465D)));
        when(chunkMapper.findAdvisableByIds(eq(List.of(7L)), anyLong())).thenReturn(List.of(verifiedChunk));
        when(documentMapper.findCurrentlyAdvisable(anyLong())).thenReturn(List.of(document(3L)));

        KnowledgeMatch match = new KnowledgeRetriever(
            properties, embeddingClient, vectorStore, chunkMapper, documentMapper
        ).retrieve("渐进负荷原理是什么");

        assertEquals(KnowledgeMatch.Status.MATCHED, match.status());
        assertEquals(7L, match.citations().get(0).chunkId());
    }

    @Test
    void rebuildAllIndexesRecreatesCollectionWhenNoChunksAreIndexable() {
        KnowledgeProperties properties = new KnowledgeProperties();
        properties.setEnabled(true);
        EmbeddingClient embeddingClient = mock(EmbeddingClient.class);
        VectorStore vectorStore = mock(VectorStore.class);
        KnowledgeChunkMapper chunkMapper = mock(KnowledgeChunkMapper.class);
        KnowledgeDocumentMapper documentMapper = mock(KnowledgeDocumentMapper.class);
        when(chunkMapper.findIndexable(anyLong())).thenReturn(List.of());

        int indexed = new KnowledgeRetriever(
            properties, embeddingClient, vectorStore, chunkMapper, documentMapper
        ).rebuildAllIndexes();

        assertEquals(0, indexed);
        verify(vectorStore).recreateCollection(properties.getOllama().getVectorDimension());
        verify(chunkMapper).markCurrentIndexesStale(anyLong(), anyLong());
    }

    private KnowledgeChunk chunk(Long id, Long documentId, String text) {
        KnowledgeChunk chunk = new KnowledgeChunk();
        chunk.setId(id);
        chunk.setDocumentId(documentId);
        chunk.setChunkText(text);
        return chunk;
    }

    private KnowledgeDocument document(Long id) {
        KnowledgeDocument document = new KnowledgeDocument();
        document.setId(id);
        document.setTitle("基础训练原则");
        document.setSourceName("健录审核知识组");
        document.setSourceUrl("https://example.invalid/source");
        document.setVersion("2026.03-p0");
        return document;
    }
}
