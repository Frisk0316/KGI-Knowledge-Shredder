package com.kgi.shredder.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "trainers")
public class Trainer {
    @Id
    @Column(name = "trainer_id", length = 64)
    private String trainerId;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(name = "role_name", nullable = false)
    private String roleName = "TRAINER";

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    protected Trainer() {
    }

    public Trainer(String trainerId, String displayName) {
        this.trainerId = trainerId;
        this.displayName = displayName;
    }

    public String getTrainerId() {
        return trainerId;
    }

    public String getDisplayName() {
        return displayName;
    }
}
