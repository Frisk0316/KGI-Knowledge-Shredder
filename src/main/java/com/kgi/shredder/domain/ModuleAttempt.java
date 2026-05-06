package com.kgi.shredder.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "module_attempts")
public class ModuleAttempt {
    @Id
    @GeneratedValue
    @Column(name = "attempt_id")
    private UUID attemptId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "module_id", nullable = false)
    private MicroModule microModule;

    @Column(name = "learner_id", nullable = false)
    private String learnerId;

    @Column(name = "trainer_id", nullable = false)
    private String trainerId;

    @Column(name = "score", nullable = false)
    private BigDecimal score;

    @Column(name = "interaction_seconds", nullable = false)
    private int interactionSeconds;

    @Column(name = "answered_at", nullable = false)
    private OffsetDateTime answeredAt = OffsetDateTime.now();

    protected ModuleAttempt() {
    }

    public ModuleAttempt(MicroModule microModule, String learnerId, String trainerId, BigDecimal score, int interactionSeconds) {
        this.microModule = microModule;
        this.learnerId = learnerId;
        this.trainerId = trainerId;
        this.score = score;
        this.interactionSeconds = interactionSeconds;
    }

    public UUID getAttemptId() {
        return attemptId;
    }
}
