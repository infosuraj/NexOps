package com.nexops.supplier.repository;

import com.nexops.supplier.entity.PurchaseOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {
    List<PurchaseOrder> findByStatus(PurchaseOrder.OrderStatus status);
    boolean existsByProductIdAndStatus(Long productId, PurchaseOrder.OrderStatus status);
}
