package com.nexops.escalation.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "support_tickets")
@Data
@NoArgsConstructor
public class SupportTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String customerName;

    @Column(nullable = false)
    private String customerEmail;

    @Column(nullable = false, length = 2000)
    private String issueDescription;

    @Enumerated(EnumType.STRING)
    private Priority priority;

    @Enumerated(EnumType.STRING)
    private Status status;

    @Column(length = 2000)
    private String aiAnalysis;

    // Gemini-drafted response for the agent to review and send
    @Column(length = 3000)
    private String draftResponse;

    private String assignedTo;
    private String assignedAgentEmail;
    private Long assignedAgentId;

    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;

    public enum Priority { LOW, MEDIUM, HIGH, CRITICAL }
    public enum Status { OPEN, IN_PROGRESS, PENDING, AUTO_RESOLVED, ESCALATED, CLOSED }

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) status = Status.OPEN;
    }
}
