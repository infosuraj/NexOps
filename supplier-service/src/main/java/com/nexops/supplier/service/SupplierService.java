package com.nexops.supplier.service;

import com.nexops.supplier.entity.PurchaseOrder;
import com.nexops.supplier.repository.PurchaseOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SupplierService {

    private final PurchaseOrderRepository repo;
    private final ResendEmailService resendEmailService;

    private static final String[] SUPPLIERS = {
        "Global Supplies Co.",
        "FastShip India Ltd.",
        "Prime Wholesale Pvt.",
        "QuickStock Solutions"
    };

    // demo supplier emails - in prod these come from a supplier DB
    private static final String[] SUPPLIER_EMAILS = {
        "supplier@globalsupplies.demo",
        "orders@fastship.demo",
        "procurement@primewholesale.demo",
        "orders@quickstock.demo"
    };

    @Transactional
    public PurchaseOrder handleLowStockAlert(Long productId, String productName,
                                              int currentQty, int threshold) {
        // saga idempotency check - don't create duplicate POs
        boolean alreadyPending = repo.existsByProductIdAndStatus(
            productId, PurchaseOrder.OrderStatus.PENDING
        );

        if (alreadyPending) {
            log.info("Skipping PO for {} - one already pending (saga guard)", productName);
            return null;
        }

        // order 3x the threshold qty to cover ~30 days
        int orderQty = threshold * 3;
        String supplier = SUPPLIERS[(int) (productId % SUPPLIERS.length)];

        // rough cost estimate at Rs 500/unit average
        BigDecimal cost = BigDecimal.valueOf(orderQty * 500L);

        PurchaseOrder po = new PurchaseOrder();
        po.setProductId(productId);
        po.setProductName(productName);
        po.setQuantityOrdered(orderQty);
        po.setSupplierName(supplier);
        po.setEstimatedCost(cost);
        po.setTriggeredBy("AUTO: low stock alert (qty=" + currentQty + ", threshold=" + threshold + ")");

        PurchaseOrder saved = repo.save(po);
        log.info("Auto PO created: #{} - {} units of {} from {}",
            saved.getId(), orderQty, productName, supplier);

        // send automated email to supplier
        int supplierIdx = (int) (productId % SUPPLIER_EMAILS.length);
        resendEmailService.sendPurchaseOrderEmail(saved, SUPPLIER_EMAILS[supplierIdx]);

        return saved;
    }

    @Transactional
    public PurchaseOrder createManual(PurchaseOrder order) {
        order.setTriggeredBy("MANUAL");
        return repo.save(order);
    }

    @Transactional
    public PurchaseOrder updateStatus(Long id, PurchaseOrder.OrderStatus newStatus) {
        PurchaseOrder po = repo.findById(id)
            .orElseThrow(() -> new RuntimeException("PO not found: " + id));
        po.setStatus(newStatus);
        return repo.save(po);
    }

    public List<PurchaseOrder> getAll() {
        return repo.findAll();
    }
}
