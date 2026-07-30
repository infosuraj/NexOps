package com.nexops.pricing.kafka;

import com.nexops.pricing.service.PricingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class DemandEventConsumer {

    private final PricingService pricingService;

    @KafkaListener(topics = "nexops.demand.event", groupId = "pricing-agent-group")
    public void onDemandEvent(Map<String, Object> event) {
        try {
            Long productId = Long.valueOf(event.get("productId").toString());
            String productName = event.get("productName").toString();
            pricingService.recordDemand(productId, productName);
        } catch (Exception e) {
            log.error("Failed to process demand event: {}", e.getMessage());
        }
    }

    @KafkaListener(topics = "nexops.stock.update", groupId = "pricing-agent-group")
    public void onStockUpdate(Map<String, Object> event) {
        try {
            Long productId = Long.valueOf(event.get("productId").toString());
            String productName = event.get("productName").toString();
            int newQty = Integer.parseInt(event.get("newQuantity").toString());
            pricingService.adjustForStock(productId, productName, newQty);
        } catch (Exception e) {
            log.error("Failed to process stock event: {}", e.getMessage());
        }
    }
}
