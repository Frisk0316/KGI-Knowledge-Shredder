package com.kgi.shredder.api.v1;

import com.kgi.shredder.api.v1.dto.DomainResponse;
import com.kgi.shredder.service.document.DomainService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/domains")
public class DomainsController {
    private final DomainService domainService;

    public DomainsController(DomainService domainService) {
        this.domainService = domainService;
    }

    @GetMapping
    public List<DomainResponse> listDomains() {
        return domainService.listDomains().stream()
                .map(domain -> new DomainResponse(domain.getDomainId(), domain.getDomainName(), domain.getDescription()))
                .toList();
    }
}
