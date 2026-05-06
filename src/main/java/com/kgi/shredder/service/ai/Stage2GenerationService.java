package com.kgi.shredder.service.ai;

import java.util.List;

public interface Stage2GenerationService {
    MicroModuleSet generateModules(String redactedText, List<String> domainNames);
}
