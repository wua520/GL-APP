package com.fitness.server.knowledge;

import java.util.List;

public interface VectorStore {
    List<VectorHit> search(List<Double> vector, int limit);
    void upsert(long pointId, long chunkId, List<Double> vector);
    void recreateCollection(int vectorDimension);

    record VectorHit(Long chunkId, double score) { }
}
