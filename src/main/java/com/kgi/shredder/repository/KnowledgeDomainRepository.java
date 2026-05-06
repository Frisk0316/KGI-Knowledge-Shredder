package com.kgi.shredder.repository;

import com.kgi.shredder.domain.KnowledgeDomain;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KnowledgeDomainRepository extends JpaRepository<KnowledgeDomain, Long> {
    List<KnowledgeDomain> findByDomainIdIn(Collection<Long> domainIds);
}
