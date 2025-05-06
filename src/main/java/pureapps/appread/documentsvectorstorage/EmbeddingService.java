package pureapps.appread.documentsvectorstorage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;
import pureapps.appread.documentsvectorstorage.dto.DocumentChunk;
import pureapps.appread.documentsvectorstorage.dto.DocumentChunkWithEmbedding;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
class EmbeddingService {
    private final EmbeddingModel embeddingModel;
    private final ExecutorService executorService = Executors.newFixedThreadPool(
            Math.max(2, Runtime.getRuntime().availableProcessors() - 1)
    );

    List<DocumentChunkWithEmbedding> generateEmbeddings(List<DocumentChunk> chunks) {
        log.info("Generating embeddings for {} chunks using {}", chunks.size(), embeddingModel.getClass().getSimpleName());

        List<CompletableFuture<DocumentChunkWithEmbedding>> futures = chunks.stream()
                .map(chunk -> CompletableFuture.supplyAsync(() -> generateEmbedding(chunk), executorService))
                .toList();

        List<DocumentChunkWithEmbedding> results = futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());

        log.info("Successfully generated embeddings for {} chunks.", results.size());
        return results;
    }

    DocumentChunkWithEmbedding generateEmbedding(DocumentChunk chunk) {
        try {
            float[] embeddingFloatArray = embeddingModel.embed(chunk.getContent());

            DocumentChunkWithEmbedding result = new DocumentChunkWithEmbedding();
            result.setContent(chunk.getContent());
            result.setFilePath(chunk.getFilePath());
            result.setStartLine(chunk.getStartLine());
            result.setEndLine(chunk.getEndLine());
            result.setEmbedding(embeddingFloatArray);

            log.debug("Generated embedding for chunk: {} (lines {}-{})", chunk.getFilePath(), chunk.getStartLine(), chunk.getEndLine());
            return result;

        } catch (Exception e) {
            log.error("Error generating embedding for chunk: {} (lines {}-{}): {}",
                    chunk.getFilePath(), chunk.getStartLine(), chunk.getEndLine(), e.getMessage());

            throw new RuntimeException("Failed to generate embedding for chunk: " + chunk.getFilePath(), e);

        }
    }

    float[] generateEmbedding(String query) {
        try {
            return embeddingModel.embed(query);

        } catch (Exception e) {
            log.error("Error generating embedding for query: {}",
                    query);
            throw new RuntimeException("Failed to generate embedding for query: " + query, e);

        }
    }
}