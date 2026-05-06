package com.kgi.shredder.service.ai;

public interface Stage1RedactionService {
    Stage1Result redactAndClassify(String rawText);
}
