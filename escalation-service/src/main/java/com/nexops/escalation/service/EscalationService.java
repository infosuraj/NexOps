package com.nexops.escalation.service;

import com.nexops.escalation.entity.SupportAgent;
import com.nexops.escalation.entity.SupportTicket;
import com.nexops.escalation.entity.SupportTicket.Priority;
import com.nexops.escalation.entity.SupportTicket.Status;
import com.nexops.escalation.repository.SupportAgentRepository;
import com.nexops.escalation.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EscalationService {

    private final TicketRepository ticketRepository;
    private final SupportAgentRepository agentRepository;
    private final GeminiAIService geminiAIService;
    private final ResendEmailService resendEmailService;

    @Transactional
    public SupportTicket processTicket(SupportTicket ticket) {
        // step 1: AI triage
        String analysis = geminiAIService.analyzeTicket(
            ticket.getCustomerName(), ticket.getIssueDescription()
        );
        Priority priority = determinePriority(ticket.getIssueDescription(), analysis);
        String category = extractCategory(analysis);

        ticket.setAiAnalysis(analysis);
        ticket.setPriority(priority);

        // step 2: Gemini draft response
        String draft = geminiAIService.draftResponse(
            ticket.getCustomerName(), ticket.getIssueDescription(),
            priority.name(), category
        );
        ticket.setDraftResponse(draft);

        // step 3: smart agent assignment based on priority + load
        SupportAgent agent = assignAgent(priority, category);

        switch (priority) {
            case LOW -> {
                if (agent != null && agent.getCurrentLoad() < 2) {
                    // agent is free — assign as pending, activates when load frees up
                    assignToAgent(ticket, agent, Status.PENDING);
                } else {
                    ticket.setStatus(Status.AUTO_RESOLVED);
                    ticket.setAssignedTo("AI Agent (auto-resolved)");
                    ticket.setResolvedAt(LocalDateTime.now());
                    // still send customer the drafted reply
                    resendEmailService.sendCustomerAutoReply(
                        ticket.getCustomerEmail(), ticket.getCustomerName(),
                        0L, draft
                    );
                }
            }
            case CRITICAL -> {
                // assign to least loaded agent regardless of specialization
                SupportAgent critAgent = agentRepository.findAvailableOrderByLoad()
                    .stream().findFirst().orElse(null);
                if (critAgent != null) {
                    assignToAgent(ticket, critAgent, Status.ESCALATED);
                } else {
                    ticket.setStatus(Status.ESCALATED);
                    ticket.setAssignedTo("Senior Support Team — all agents at capacity");
                }
                log.warn("CRITICAL ticket — escalated immediately");
            }
            case HIGH -> {
                if (agent != null) {
                    assignToAgent(ticket, agent, Status.IN_PROGRESS);
                } else {
                    ticket.setStatus(Status.IN_PROGRESS);
                    ticket.setAssignedTo("Support Team Level 2 (queue)");
                }
            }
            default -> { // MEDIUM
                if (agent != null) {
                    assignToAgent(ticket, agent, Status.IN_PROGRESS);
                } else {
                    ticket.setStatus(Status.IN_PROGRESS);
                    ticket.setAssignedTo("Support Team Level 1 (queue)");
                }
            }
        }

        SupportTicket saved = ticketRepository.save(ticket);
        log.info("Ticket #{} → priority={}, status={}, assigned={}",
            saved.getId(), priority, saved.getStatus(), saved.getAssignedTo());
        return saved;
    }

    private void assignToAgent(SupportTicket ticket, SupportAgent agent, Status status) {
        ticket.setStatus(status);
        ticket.setAssignedTo(agent.getName() + " (" + agent.getSpecialization() + ")");
        ticket.setAssignedAgentId(agent.getId());
        ticket.setAssignedAgentEmail(agent.getEmail());

        agent.setCurrentLoad(agent.getCurrentLoad() + 1);
        agentRepository.save(agent);

        // notify agent by email with the AI draft
        resendEmailService.sendTicketAssignmentEmail(
            agent.getEmail(), agent.getName(),
            ticket.getId() != null ? ticket.getId() : 0L,
            ticket.getCustomerName(), ticket.getIssueDescription(),
            ticket.getDraftResponse()
        );

        // send auto-reply to customer
        resendEmailService.sendCustomerAutoReply(
            ticket.getCustomerEmail(), ticket.getCustomerName(),
            ticket.getId() != null ? ticket.getId() : 0L,
            ticket.getDraftResponse()
        );
    }

    private SupportAgent assignAgent(Priority priority, String category) {
        // try to find specialist first
        List<SupportAgent> specialists = agentRepository.findAvailableBySpecialization(category);
        if (!specialists.isEmpty()) return specialists.get(0);

        // fallback: any available agent
        List<SupportAgent> available = agentRepository.findAvailableOrderByLoad();
        return available.isEmpty() ? null : available.get(0);
    }

    private Priority determinePriority(String issue, String aiAnalysis) {
        String combined = ((issue != null ? issue : "") + " " + (aiAnalysis != null ? aiAnalysis : "")).toLowerCase();
        if (combined.contains("critical") || combined.contains("urgent") ||
            combined.contains("asap") || combined.contains("immediately") ||
            combined.contains("security") || combined.contains("data loss"))
            return Priority.CRITICAL;
        if (combined.contains("refund") || combined.contains("financial") ||
            combined.contains("broken") || combined.contains("not working") || combined.contains("high"))
            return Priority.HIGH;
        if (combined.contains("slow") || combined.contains("issue") || combined.contains("medium"))
            return Priority.MEDIUM;
        return Priority.LOW;
    }

    private String extractCategory(String analysis) {
        if (analysis.contains("FINANCIAL")) return "FINANCIAL";
        if (analysis.contains("TECHNICAL")) return "TECHNICAL";
        if (analysis.contains("RETENTION")) return "RETENTION";
        return "GENERAL";
    }

    public List<SupportTicket> getAll() {
        return ticketRepository.findAll();
    }

    public List<SupportTicket> getByStatus(Status status) {
        return ticketRepository.findByStatus(status);
    }

    public List<SupportAgent> getAgents() {
        return agentRepository.findAll();
    }
}
