package com.kgi.shredder.service.document;

import com.kgi.shredder.domain.KnowledgeDomain;
import com.kgi.shredder.repository.KnowledgeDomainRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class DomainService {
    private final KnowledgeDomainRepository knowledgeDomainRepository;

    public DomainService(KnowledgeDomainRepository knowledgeDomainRepository) {
        this.knowledgeDomainRepository = knowledgeDomainRepository;
    }

    public List<KnowledgeDomain> listDomains() {
        return knowledgeDomainRepository.findAll();
    }
}
