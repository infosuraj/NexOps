package com.nexops.escalation.config;

import com.nexops.escalation.entity.SupportAgent;
import com.nexops.escalation.repository.SupportAgentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final SupportAgentRepository agentRepo;

    @Override
    public void run(String... args) {
        if (agentRepo.count() > 0) return; // already seeded

        String[][] agents = {
            {"Priya Sharma",   "priya@nexops.internal",   "FINANCIAL",  "5"},
            {"Rahul Verma",    "rahul@nexops.internal",   "TECHNICAL",  "5"},
            {"Ananya Singh",   "ananya@nexops.internal",  "GENERAL",    "8"},
            {"Kiran Kumar",    "kiran@nexops.internal",   "RETENTION",  "5"},
            {"Arjun Nair",     "arjun@nexops.internal",   "TECHNICAL",  "5"},
        };

        for (String[] a : agents) {
            SupportAgent agent = new SupportAgent();
            agent.setName(a[0]);
            agent.setEmail(a[1]);
            agent.setSpecialization(a[2]);
            agent.setMaxLoad(Integer.parseInt(a[3]));
            agentRepo.save(agent);
        }

        log.info("Seeded {} support agents", agents.length);
    }
}
