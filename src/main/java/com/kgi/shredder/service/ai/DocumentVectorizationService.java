package com.kgi.shredder.service.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kgi.shredder.domain.DocumentChunk;
import com.kgi.shredder.domain.DocumentDomainMap;
import com.kgi.shredder.domain.DocumentVersion;
import com.kgi.shredder.domain.SourceDocument;
import com.kgi.shredder.repository.DocumentChunkRepository;
import com.kgi.shredder.repository.DocumentDomainMapRepository;
import com.kgi.shredder.service.chunking.ChunkingOrchestrator;
import com.kgi.shredder.service.chunking.DocumentChunkCandidate;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class DocumentVectorizationService {
    private final ChunkingOrchestrator chunkingOrchestrator;
    private final DocumentChunkRepository documentChunkRepository;
    private final DocumentDomainMapRepository documentDomainMapRepository;
    private final EmbeddingGateway embeddingGateway;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public DocumentVectorizationService(
            ChunkingOrchestrator chunkingOrchestrator,
            DocumentChunkRepository documentChunkRepository,
            DocumentDomainMapRepository documentDomainMapRepository,
            EmbeddingGateway embeddingGateway,
            JdbcTemplate jdbcTemplate,
            ObjectMapper objectMapper
    ) {
        this.chunkingOrchestrator = chunkingOrchestrator;
        this.documentChunkRepository = documentChunkRepository;
        this.documentDomainMapRepository = documentDomainMapRepository;
        this.embeddingGateway = embeddingGateway;
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public List<DocumentChunk> vectorize(SourceDocument sourceDocument, DocumentVersion version) {
        documentChunkRepository.deleteByDocumentVersionVersionId(version.getVersionId());
        jdbcTemplate.update("DELETE FROM document_embeddings WHERE metadata->>'version_id' = ?", version.getVersionId().toString());

        String sourceText = version.getRedactedText() == null || version.getRedactedText().isBlank()
                ? version.getRawText()
                : version.getRedactedText();
        List<DocumentChunkCandidate> candidates = chunkingOrchestrator.chunk(sourceText, version.getClassification());
        List<Long> domainIds = documentDomainMapRepository.findBySourceDocumentDocId(sourceDocument.getDocId()).stream()
                .map(DocumentDomainMap::getKnowledgeDomain)
                .map(domain -> domain.getDomainId())
                .toList();

        List<DocumentChunk> chunks = documentChunkRepository.saveAll(candidates.stream()
                .map(candidate -> new DocumentChunk(version, candidate.index(), candidate.text(), candidate.strategy()))
                .toList());
        for (DocumentChunk chunk : chunks) {
            UUID vectorStoreId = UUID.randomUUID();
            Map<String, ?> metadata = Map.of(
                    "trainer_id", sourceDocument.getTrainer().getTrainerId(),
                    "doc_id", sourceDocument.getDocId().toString(),
                    "version_id", version.getVersionId().toString(),
                    "chunk_id", chunk.getChunkId().toString(),
                    "domain_ids", domainIds
            );
            insertEmbedding(vectorStoreId, chunk.getChunkText(), metadata, embeddingGateway.embed(chunk.getChunkText()));
            chunk.markVectorStored(vectorStoreId);
        }
        return documentChunkRepository.saveAll(chunks);
    }

    private void insertEmbedding(UUID id, String content, Map<String, ?> metadata, float[] embedding) {
        try {
            jdbcTemplate.update(
                    "INSERT INTO document_embeddings (id, content, metadata, embedding) VALUES (?, ?, ?::jsonb, ?::vector)",
                    id,
                    content,
                    objectMapper.writeValueAsString(metadata),
                    vectorLiteral(embedding)
            );
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to write document embedding.", ex);
        }
    }

    static String vectorLiteral(float[] embedding) {
        StringBuilder builder = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append(embedding[i]);
        }
        return builder.append(']').toString();
    }
}
