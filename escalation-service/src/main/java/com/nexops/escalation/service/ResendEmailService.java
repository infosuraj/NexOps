package com.nexops.escalation.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import java.util.Map;

@Service
@Slf4j
public class ResendEmailService {

    @Value("${resend.api.key}")
    private String apiKey;

    @Value("${resend.api.from}")
    private String fromEmail;

    private final WebClient webClient;

    public ResendEmailService(WebClient.Builder builder) {
        this.webClient = builder.baseUrl("https://api.resend.com").build();
    }

    public void sendEmail(String to, String subject, String html) {
        if ("demo-mode".equals(apiKey)) {
            log.info("[DEMO] Email to {} | Subject: {}", to, subject);
            return;
        }

        try {
            webClient.post()
                .uri("/emails")
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .bodyValue(Map.of(
                    "from", fromEmail,
                    "to",   new String[]{to},
                    "subject", subject,
                    "html", html
                ))
                .retrieve()
                .bodyToMono(Map.class)
                .subscribe(
                    res -> log.info("Email sent to {}: {}", to, res.get("id")),
                    err -> log.error("Failed to send email to {}: {}", to, err.getMessage())
                );
        } catch (Exception e) {
            log.error("Resend error: {}", e.getMessage());
        }
    }

    public void sendTicketAssignmentEmail(String agentEmail, String agentName,
                                          Long ticketId, String customerName,
                                          String issue, String draft) {
        String html = """
            <div style="font-family:Helvetica,sans-serif;max-width:600px;margin:0 auto">
              <div style="background:#1557FF;padding:20px;border-radius:8px 8px 0 0">
                <h2 style="color:#fff;margin:0">NexOps Support</h2>
              </div>
              <div style="background:#fff;padding:24px;border:1px solid #e8e8e8;border-radius:0 0 8px 8px">
                <p>Hi <b>%s</b>,</p>
                <p>Ticket <b>#%d</b> has been assigned to you.</p>
                <hr style="border:none;border-top:1px solid #f0f0f0;margin:16px 0"/>
                <p><b>Customer:</b> %s</p>
                <p><b>Issue:</b> %s</p>
                <hr style="border:none;border-top:1px solid #f0f0f0;margin:16px 0"/>
                <p><b>AI-Drafted Response (review before sending):</b></p>
                <div style="background:#f3f3f3;padding:16px;border-radius:6px;font-size:14px">%s</div>
                <p style="margin-top:20px;font-size:12px;color:#a0a0a0">
                  Login to the NexOps Admin Console to update ticket status.
                </p>
              </div>
            </div>
            """.formatted(agentName, ticketId, customerName, issue, draft);

        sendEmail(agentEmail,
            "Ticket #" + ticketId + " assigned to you — " + customerName,
            html);
    }

    public void sendCustomerAutoReply(String customerEmail, String customerName,
                                      Long ticketId, String draft) {
        String html = """
            <div style="font-family:Helvetica,sans-serif;max-width:600px;margin:0 auto">
              <div style="background:#111;padding:20px;border-radius:8px 8px 0 0">
                <h2 style="color:#fff;margin:0">NexOps Support</h2>
              </div>
              <div style="background:#fff;padding:24px;border:1px solid #e8e8e8;border-radius:0 0 8px 8px">
                <p>Hi <b>%s</b>,</p>
                <p>We received your support request (Ticket <b>#%d</b>) and our team is on it.</p>
                <hr style="border:none;border-top:1px solid #f0f0f0;margin:16px 0"/>
                %s
                <p style="margin-top:20px;font-size:12px;color:#a0a0a0">
                  A support agent will follow up with you shortly.
                  — NexOps Support Team
                </p>
              </div>
            </div>
            """.formatted(customerName, ticketId, draft);

        sendEmail(customerEmail, "We received your request — Ticket #" + ticketId, html);
    }
}
