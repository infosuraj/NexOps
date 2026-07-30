package com.nexops.inventory.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class InventoryEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${nexops.kafka.topics.stock-update}")
    private String stockUpdateTopic;

    @Value("${nexops.kafka.topics.low-stock}")
    private String lowStockTopic;

    @Value("${nexops.kafka.topics.demand-event}")
    private String demandEventTopic;

    public void publishStockUpdate(Long productId, String name, int oldQty, int newQty) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "STOCK_UPDATED");
        event.put("productId", productId);
        event.put("productName", name);
        event.put("oldQuantity", oldQty);
        event.put("newQuantity", newQty);
        event.put("timestamp", System.currentTimeMillis());

        kafkaTemplate.send(stockUpdateTopic, String.valueOf(productId), event);
        log.info("Published stock update for product {}: {} -> {}", name, oldQty, newQty);
    }

    public void publishLowStockAlert(Long productId, String name, int currentQty, int threshold) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "LOW_STOCK_ALERT");
        event.put("productId", productId);
        event.put("productName", name);
        event.put("currentQuantity", currentQty);
        event.put("threshold", threshold);
        event.put("timestamp", System.currentTimeMillis());

        kafkaTemplate.send(lowStockTopic, String.valueOf(productId), event);
        log.warn("LOW STOCK alert published for {} (qty={}, threshold={})", name, currentQty, threshold);
    }

    // called every time a purchase is made so pricing agent can track demand
    public void publishDemandEvent(Long productId, String name) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "PURCHASE_MADE");
        event.put("productId", productId);
        event.put("productName", name);
        event.put("timestamp", System.currentTimeMillis());

        kafkaTemplate.send(demandEventTopic, String.valueOf(productId), event);
    }
}
