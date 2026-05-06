package com.kgi.shredder.repository;

import com.kgi.shredder.domain.ModuleAttempt;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ModuleAttemptRepository extends JpaRepository<ModuleAttempt, UUID> {
}
