package com.fitness.server.knowledge;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class KnowledgeIndexStartupRunner implements ApplicationRunner {
    private static final Logger logger = LoggerFactory.getLogger(KnowledgeIndexStartupRunner.class);

    private final KnowledgeProperties properties;
    private final VerifiedKnowledgeSeedImporter seedImporter;
    private final KnowledgeRetriever retriever;

    public KnowledgeIndexStartupRunner(KnowledgeProperties properties, VerifiedKnowledgeSeedImporter seedImporter,
                                       KnowledgeRetriever retriever) {
        this.properties = properties;
        this.seedImporter = seedImporter;
        this.retriever = retriever;
    }

    @Override
    public void run(ApplicationArguments arguments) {
        if (!properties.isEnabled()) {
            return;
        }
        try {
            if (properties.isImportSeedsOnStart()) {
                logger.info("Imported {} changed verified knowledge seed(s)", seedImporter.importApprovedSeeds());
            }
            if (properties.isRebuildOnStart()) {
                int indexed = retriever.rebuildAllIndexes();
                logger.info("Knowledge index rebuilt with {} chunk(s)", indexed);
            }
        } catch (Exception exception) {
            logger.warn("Knowledge startup maintenance skipped: {}", exception.getMessage());
        }
    }
}
