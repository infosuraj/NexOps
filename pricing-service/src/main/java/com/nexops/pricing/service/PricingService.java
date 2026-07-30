package com.nexops.pricing.service;

import com.nexops.pricing.entity.PricingRule;
import com.nexops.pricing.repository.PricingRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
@Slf4j
public class PricingService {

    private final PricingRuleRepository repo;
    private final RestTemplate restTemplate;

    @Value("${nexops.pricing.demand-threshold}")
    private int demandThreshold;

    @Value("${nexops.pricing.max-increase-pct}")
    private int maxIncreasePct;

    @Value("${inventory.service.url}")
    private String inventoryUrl;

    // in-memory demand counters per product - resets after each price adjustment
    private final ConcurrentHashMap<Long, AtomicInteger> demandCounters = new ConcurrentHashMap<>();

    @Transactional
    public void recordDemand(Long productId, String productName) {
        int count = demandCounters
                .computeIfAbsent(productId, k -> new AtomicInteger(0))
                .incrementAndGet();

        log.debug("Demand count for {}: {}", productName, count);

        if (count >= demandThreshold) {
            BigDecimal current = fetchCurrentPrice(productId);
            // increase proportional to demand, but cap at maxIncreasePct
            double pct = Math.min(count * 2.5, maxIncreasePct);
            BigDecimal newPrice = current.multiply(
                BigDecimal.ONE.add(BigDecimal.valueOf(pct / 100))
            ).setScale(2, RoundingMode.HALF_UP);

            applyPriceChange(productId, productName, current, newPrice,
                    "HIGH_DEMAND: " + count + " purchases -> +" + String.format("%.1f", pct) + "%");

            demandCounters.get(productId).set(0); // reset after adjustment
        }
    }

    @Transactional
    public void adjustForStock(Long productId, String productName, int newQty) {
        BigDecimal current = fetchCurrentPrice(productId);

        if (newQty <= 5) {
            // very low stock - scarcity pricing
            BigDecimal newPrice = current.multiply(BigDecimal.valueOf(1.15)).setScale(2, RoundingMode.HALF_UP);
            applyPriceChange(productId, productName, current, newPrice,
                    "SCARCITY: only " + newQty + " units left (+15%)");

        } else if (newQty > 100) {
            // overstocked - push sales with discount
            BigDecimal newPrice = current.multiply(BigDecimal.valueOf(0.90)).setScale(2, RoundingMode.HALF_UP);
            applyPriceChange(productId, productName, current, newPrice,
                    "OVERSTOCK: " + newQty + " units available (-10%)");
        }
        // between 5-100: no price change needed
    }

    private void applyPriceChange(Long productId, String name,
                                   BigDecimal oldPrice, BigDecimal newPrice, String reason) {
        // save the decision record
        PricingRule rule = new PricingRule();
        rule.setProductId(productId);
        rule.setProductName(name);
        rule.setPreviousPrice(oldPrice);
        rule.setNewPrice(newPrice);
        rule.setReason(reason);
        rule.setDemandCount(demandCounters.getOrDefault(productId, new AtomicInteger(0)).get());
        repo.save(rule);

        // push to inventory service
        try {
            String url = inventoryUrl + "/api/inventory/products/" + productId + "/price";
            restTemplate.put(url, Map.of("price", newPrice));
            log.info("Price updated for {}: {} -> {} | {}", name, oldPrice, newPrice, reason);
        } catch (Exception e) {
            log.error("Couldn't update price in inventory service: {}", e.getMessage());
        }
    }

    private BigDecimal fetchCurrentPrice(Long productId) {
        try {
            String url = inventoryUrl + "/api/inventory/products/" + productId;
            Map<?, ?> product = restTemplate.getForObject(url, Map.class);
            if (product != null && product.get("currentPrice") != null) {
                return new BigDecimal(product.get("currentPrice").toString());
            }
        } catch (Exception e) {
            log.warn("Couldn't fetch price for product {}, using fallback", productId);
        }
        return BigDecimal.valueOf(100.00);
    }

    public List<PricingRule> getHistory() {
        return repo.findAll();
    }

    public List<PricingRule> getHistoryForProduct(Long productId) {
        return repo.findByProductIdOrderByDecidedAtDesc(productId);
    }
}
