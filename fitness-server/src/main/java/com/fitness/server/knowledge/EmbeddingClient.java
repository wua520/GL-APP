package com.fitness.server.knowledge;

import java.util.List;

public interface EmbeddingClient {
    List<Double> embed(String text);
}
