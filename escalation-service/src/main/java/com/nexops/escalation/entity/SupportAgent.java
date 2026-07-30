package com.nexops.escalation.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "support_agents")
@Data
@NoArgsConstructor
public class SupportAgent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String email;

    // TECHNICAL, FINANCIAL, GENERAL, RETENTION
    private String specialization;

    private int maxLoad = 5;

    // incremented when ticket assigned, decremented when closed
    private int currentLoad = 0;

    public boolean isAvailable() {
        return currentLoad < maxLoad;
    }
}
