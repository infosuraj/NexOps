package com.nexops.escalation.controller;

import com.nexops.escalation.entity.SupportAgent;
import com.nexops.escalation.entity.SupportTicket;
import com.nexops.escalation.service.EscalationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/escalation")
@RequiredArgsConstructor
public class EscalationController {

    private final EscalationService escalationService;

    @PostMapping("/tickets")
    public ResponseEntity<SupportTicket> create(@RequestBody SupportTicket ticket) {
        return ResponseEntity.ok(escalationService.processTicket(ticket));
    }

    @GetMapping("/tickets")
    public ResponseEntity<List<SupportTicket>> all() {
        return ResponseEntity.ok(escalationService.getAll());
    }

    @GetMapping("/tickets/status/{status}")
    public ResponseEntity<List<SupportTicket>> byStatus(@PathVariable String status) {
        return ResponseEntity.ok(escalationService.getByStatus(
            SupportTicket.Status.valueOf(status.toUpperCase())
        ));
    }

    @GetMapping("/agents")
    public ResponseEntity<List<SupportAgent>> agents() {
        return ResponseEntity.ok(escalationService.getAgents());
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "escalation-service",
            "agent", "Gemini AI Escalation Agent ACTIVE"
        ));
    }
}
