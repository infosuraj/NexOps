package com.nexops.escalation.repository;

import com.nexops.escalation.entity.SupportTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TicketRepository extends JpaRepository<SupportTicket, Long> {
    List<SupportTicket> findByStatus(SupportTicket.Status status);
    List<SupportTicket> findByPriority(SupportTicket.Priority priority);
}
