package com.nexops.escalation.repository;

import com.nexops.escalation.entity.SupportAgent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface SupportAgentRepository extends JpaRepository<SupportAgent, Long> {

    // least loaded agent overall
    @Query("SELECT a FROM SupportAgent a WHERE a.currentLoad < a.maxLoad ORDER BY a.currentLoad ASC")
    List<SupportAgent> findAvailableOrderByLoad();

    // least loaded agent for a specialization
    @Query("SELECT a FROM SupportAgent a WHERE a.specialization = :spec AND a.currentLoad < a.maxLoad ORDER BY a.currentLoad ASC")
    List<SupportAgent> findAvailableBySpecialization(String spec);
}
