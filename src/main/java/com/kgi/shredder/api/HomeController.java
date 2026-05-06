package com.kgi.shredder.api;

import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {

    @GetMapping("/")
    public Map<String, Object> home() {
        return Map.of(
                "service", "KGI Knowledge Shredder API",
                "status", "running",
                "health", "/actuator/health",
                "apiBase", "/api/v1",
                "coreEndpoints", List.of(
                        "GET /api/v1/domains",
                        "POST /api/v1/documents/upload",
                        "POST /api/v1/documents/{docId}/reprocess",
                        "GET /api/v1/jobs/{jobId}",
                        "GET /api/v1/documents/{docId}/modules",
                        "POST /api/v1/rag/query"
                )
        );
    }
}
