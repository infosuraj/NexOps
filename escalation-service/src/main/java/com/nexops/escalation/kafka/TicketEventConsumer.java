package com.nexops.escalation.kafka;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

// placeholder - tickets come in via REST API directly
// future: consume from external ticket queue
@Component
@Slf4j
public class TicketEventConsumer {
    // TODO: add Kafka listener for webhook-based ticket ingestion
}
