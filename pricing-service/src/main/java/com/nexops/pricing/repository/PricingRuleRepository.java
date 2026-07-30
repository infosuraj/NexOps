package com.nexops.pricing.repository;

import com.nexops.pricing.entity.PricingRule;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PricingRuleRepository extends JpaRepository<PricingRule, Long> {
    List<PricingRule> findByProductIdOrderByDecidedAtDesc(Long productId);
}
