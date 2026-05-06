package com.kgi.shredder.service.ai;

import com.kgi.shredder.domain.DocumentVersion;
import com.kgi.shredder.domain.GenerationJob;
import com.kgi.shredder.domain.MicroModule;
import com.kgi.shredder.repository.DocumentDomainMapRepository;
import com.kgi.shredder.repository.MicroModuleRepository;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class MicroModulePersistenceService {
    private final MicroModuleRepository microModuleRepository;
    private final DocumentDomainMapRepository documentDomainMapRepository;

    public MicroModulePersistenceService(
            MicroModuleRepository microModuleRepository,
            DocumentDomainMapRepository documentDomainMapRepository
    ) {
        this.microModuleRepository = microModuleRepository;
        this.documentDomainMapRepository = documentDomainMapRepository;
    }

    @Transactional
    public List<MicroModule> replaceModules(
            DocumentVersion version,
            GenerationJob job,
            MicroModuleSet moduleSet,
            BigDecimal validationScore,
            boolean validated
    ) {
        microModuleRepository.deleteByDocumentVersionVersionId(version.getVersionId());
        UUID docId = version.getSourceDocument().getDocId();
        List<Long> documentDomainIds = documentDomainMapRepository.findBySourceDocumentDocId(docId)
                .stream()
                .map(m -> m.getKnowledgeDomain().getDomainId())
                .toList();
        List<MicroModule> modules = moduleSet.modules().stream()
                .map(item -> {
                    List<Long> domainIds = item.domainIds() != null && !item.domainIds().isEmpty()
                            ? item.domainIds()
                            : documentDomainIds;
                    return new MicroModule(
                            version,
                            job,
                            item.sequenceOrder(),
                            item.title(),
                            item.content(),
                            item.keyTakeaway(),
                            item.readingTimeMinutes(),
                            validationScore,
                            validated,
                            domainIds
                    );
                })
                .toList();
        return microModuleRepository.saveAll(modules);
    }
}
