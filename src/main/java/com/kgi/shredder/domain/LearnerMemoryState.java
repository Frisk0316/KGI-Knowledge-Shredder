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
@Table(name = "learner_memory_state")
public class LearnerMemoryState {
    @Id
    @GeneratedValue
    @Column(name = "state_id")
    private UUID stateId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "module_id", nullable = false)
    private MicroModule microModule;

    @Column(name = "learner_id", nullable = false)
    private String learnerId;

    @Column(name = "ease_factor", nullable = false)
    private BigDecimal easeFactor = BigDecimal.valueOf(2.50);

    @Column(name = "interval_days", nullable = false)
    private int intervalDays = 1;

    @Column(name = "repetitions", nullable = false)
    private int repetitions;

    @Column(name = "next_review_at", nullable = false)
    private OffsetDateTime nextReviewAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    protected LearnerMemoryState() {
    }

    public LearnerMemoryState(MicroModule microModule, String learnerId) {
        this.microModule = microModule;
        this.learnerId = learnerId;
        this.nextReviewAt = OffsetDateTime.now().plusDays(1);
    }

    public void applyAttempt(BigDecimal score) {
        double numericScore = score.doubleValue();
        if (numericScore >= 0.7) {
            repetitions += 1;
            intervalDays = repetitions == 1 ? 1 : Math.max(1, (int) Math.round(intervalDays * easeFactor.doubleValue()));
            easeFactor = easeFactor.add(BigDecimal.valueOf(0.05));
        } else {
            repetitions = 0;
            intervalDays = 1;
            easeFactor = easeFactor.subtract(BigDecimal.valueOf(0.20)).max(BigDecimal.valueOf(1.30));
        }
        nextReviewAt = OffsetDateTime.now().plusDays(intervalDays);
        updatedAt = OffsetDateTime.now();
    }

    public UUID getStateId() {
        return stateId;
    }

    public MicroModule getMicroModule() {
        return microModule;
    }

    public String getLearnerId() {
        return learnerId;
    }

    public OffsetDateTime getNextReviewAt() {
        return nextReviewAt;
    }
}
