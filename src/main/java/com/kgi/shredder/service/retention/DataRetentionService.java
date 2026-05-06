package com.kgi.shredder.service.retention;

import com.kgi.shredder.config.properties.KgiProperties;
import com.kgi.shredder.domain.SourceDocument;
import com.kgi.shredder.repository.SourceDocumentRepository;
import java.time.OffsetDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DataRetentionService {
    private static final Logger log = LoggerFactory.getLogger(DataRetentionService.class);

    private final SourceDocumentRepository sourceDocumentRepository;
    private final KgiProperties properties;

    public DataRetentionService(SourceDocumentRepository sourceDocumentRepository, KgiProperties properties) {
        this.sourceDocumentRepository = sourceDocumentRepository;
        this.properties = properties;
    }

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void purgeExpiredSoftDeletes() {
        int retentionDays = properties.retention().softDeleteRetentionDays();
        OffsetDateTime cutoff = OffsetDateTime.now().minusDays(retentionDays);
        List<SourceDocument> expired = sourceDocumentRepository.findByDeletedTrueAndUpdatedAtBefore(cutoff);
        if (!expired.isEmpty()) {
            log.info("Purging {} soft-deleted documents older than {} days.", expired.size(), retentionDays);
            sourceDocumentRepository.deleteAll(expired);
        }
    }
}
