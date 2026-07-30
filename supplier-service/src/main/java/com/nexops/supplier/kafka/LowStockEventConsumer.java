package com.nexops.supplier.kafka;

import com.nexops.supplier.service.SupplierService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class LowStockEventConsumer {

    private final SupplierService supplierService;

    @KafkaListener(topics = "nexops.stock.low", groupId = "supplier-agent-group")
    public void onLowStock(Map<String, Object> event) {
        log.warn("Received low stock alert: {}", event);
        try {
            Long productId = Long.valueOf(event.get("productId").toString());
            String productName = event.get("productName").toString();
            int currentQty = Integer.parseInt(event.get("currentQuantity").toString());
            int threshold = Integer.parseInt(event.get("threshold").toString());

            supplierService.handleLowStockAlert(productId, productName, currentQty, threshold);
        } catch (Exception e) {
            // saga compensating action - log and move on, don't crash the consumer
            log.error("Saga compensation triggered: {}", e.getMessage());
        }
    }
}
