package com.kgi.shredder.repository;

import com.kgi.shredder.domain.Trainer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TrainerRepository extends JpaRepository<Trainer, String> {
}
