package com.kgi.shredder.api.v1;

import com.kgi.shredder.api.v1.dto.SessionResponse;
import com.kgi.shredder.config.SecurityContextUtil;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/session")
public class SessionController {

    @GetMapping
    public SessionResponse get() {
        return new SessionResponse(
                SecurityContextUtil.currentActorId(),
                SecurityContextUtil.currentTrainerId(),
                SecurityContextUtil.currentAuthorities()
        );
    }
}
