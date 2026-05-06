package com.kgi.shredder.service.ai;

public interface CheckpointValidationService {
    CheckpointValidationResult validate(String redactedSource, MicroModuleSet moduleSet);
}
