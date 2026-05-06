package com.kgi.shredder.api.v1;

import com.kgi.shredder.api.v1.dto.JobResponse;
import com.kgi.shredder.config.SecurityContextUtil;
import com.kgi.shredder.domain.GenerationJob;
import com.kgi.shredder.service.job.GenerationJobOrchestrator;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/jobs")
public class JobsController {
    private final GenerationJobOrchestrator generationJobOrchestrator;

    public JobsController(GenerationJobOrchestrator generationJobOrchestrator) {
        this.generationJobOrchestrator = generationJobOrchestrator;
    }

    @GetMapping("/{jobId}")
    public JobResponse get(@PathVariable UUID jobId) {
        GenerationJob job = generationJobOrchestrator.getJob(SecurityContextUtil.currentTrainerId(), jobId);
        return new JobResponse(
                job.getJobId(),
                job.getStatus().name(),
                job.getErrorMessage(),
                job.getValidationPassed(),
                job.getValidationOutput()
        );
    }
}
