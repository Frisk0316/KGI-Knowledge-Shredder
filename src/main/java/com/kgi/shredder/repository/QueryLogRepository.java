package com.kgi.shredder.repository;

import com.kgi.shredder.domain.QueryLog;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QueryLogRepository extends JpaRepository<QueryLog, UUID> {
}
