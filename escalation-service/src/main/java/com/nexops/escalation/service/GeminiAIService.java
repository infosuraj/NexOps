package com.nexops.escalation.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class GeminiAIService {

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.url}")
    private String apiUrl;

    private final WebClient webClient;

    public GeminiAIService(WebClient.Builder builder) {
        this.webClient = builder.build();
    }

    public String analyzeTicket(String customerName, String issue) {
        if ("demo-mode".equals(apiKey)) {
            return fallbackAnalysis(issue);
        }

        String prompt = """
            You are an AI support triage agent for NexOps e-commerce platform.
            Analyze this support ticket and respond in EXACTLY this format (no extra text):

            PRIORITY: [LOW/MEDIUM/HIGH/CRITICAL]
            CATEGORY: [FINANCIAL/TECHNICAL/GENERAL/RETENTION]
            SUMMARY: [one sentence]
            ACTION: [what should happen next]

            Customer: %s
            Issue: %s
            """.formatted(customerName, issue);

        return callGemini(prompt, 200, "GEMINI TRIAGE: ", () -> fallbackAnalysis(issue));
    }

    public String draftResponse(String customerName, String issue, String priority, String category) {
        if ("demo-mode".equals(apiKey)) {
            return fallbackDraft(customerName, issue);
        }

        String prompt = """
            You are a professional customer support agent for NexOps e-commerce platform.
            Draft a helpful, empathetic response to this customer's issue.
            Be concise (3-4 sentences max). Do not mention AI.

            Customer: %s
            Issue: %s
            Priority: %s
            Category: %s

            Write only the email body (no subject, no greeting, start directly with the resolution or acknowledgment).
            """.formatted(customerName, issue, priority, category);

        return callGemini(prompt, 300, "", () -> fallbackDraft(customerName, issue));
    }

    private String callGemini(String prompt, int maxTokens, String prefix, java.util.function.Supplier<String> fallback) {
        try {
            Map<String, Object> body = Map.of(
                "contents", List.of(Map.of(
                    "parts", List.of(Map.of("text", prompt))
                )),
                "generationConfig", Map.of(
                    "temperature", 0.3,
                    "maxOutputTokens", maxTokens
                )
            );

            Map response = webClient.post()
                .uri(apiUrl + "?key=" + apiKey)
                .header("Content-Type", "application/json")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(Duration.ofSeconds(12))
                .block();

            if (response != null) {
                List candidates = (List) response.get("candidates");
                if (candidates != null && !candidates.isEmpty()) {
                    Map candidate = (Map) candidates.get(0);
                    Map content = (Map) candidate.get("content");
                    List parts = (List) content.get("parts");
                    return prefix + ((Map) parts.get(0)).get("text").toString().trim();
                }
            }
        } catch (Exception e) {
            log.warn("Gemini call failed: {}", e.getMessage());
        }
        return fallback.get();
    }

    private String fallbackAnalysis(String issue) {
        String lower = issue.toLowerCase();
        StringBuilder r = new StringBuilder("RULE-BASED: ");

        if (lower.contains("refund") || lower.contains("money") || lower.contains("charge"))
            r.append("CATEGORY: FINANCIAL. ");
        else if (lower.contains("broken") || lower.contains("error") || lower.contains("not working"))
            r.append("CATEGORY: TECHNICAL. ");
        else if (lower.contains("cancel") || lower.contains("unsubscribe"))
            r.append("CATEGORY: RETENTION. ");
        else
            r.append("CATEGORY: GENERAL. ");

        if (lower.contains("urgent") || lower.contains("asap") || lower.contains("immediately"))
            r.append("HIGH urgency detected.");

        return r.toString();
    }

    private String fallbackDraft(String customerName, String issue) {
        String lower = issue.toLowerCase();
        if (lower.contains("refund"))
            return "Thank you for reaching out about your refund request. We have escalated this to our billing team and will process your refund within 3-5 business days. You will receive a confirmation email once processed.";
        if (lower.contains("broken") || lower.contains("not working"))
            return "We apologize for the inconvenience you're experiencing. Our technical team has been notified and is investigating the issue. Please expect a resolution within 24 hours.";
        return "Thank you for contacting NexOps support. We have received your request and our team will get back to you within 24 hours with a resolution.";
    }
}
