package com.kgi.shredder.api.v1.dto;

import java.util.List;

public record SessionResponse(
        String actorId,
        String workspaceTrainerId,
        List<String> authorities
) {
}
