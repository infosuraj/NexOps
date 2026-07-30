package com.nexops.pricing.controller;

import com.nexops.pricing.entity.PricingRule;
import com.nexops.pricing.service.PricingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pricing")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PricingController {

    private final PricingService pricingService;

    @GetMapping("/history")
    public ResponseEntity<List<PricingRule>> history() {
        return ResponseEntity.ok(pricingService.getHistory());
    }

    @GetMapping("/history/{productId}")
    public ResponseEntity<List<PricingRule>> historyForProduct(@PathVariable Long productId) {
        return ResponseEntity.ok(pricingService.getHistoryForProduct(productId));
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "pricing-service",
            "agent", "Dynamic Pricing Agent ACTIVE"
        ));
    }
}
