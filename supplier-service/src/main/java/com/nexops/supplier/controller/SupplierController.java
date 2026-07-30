package com.nexops.supplier.controller;

import com.nexops.supplier.entity.PurchaseOrder;
import com.nexops.supplier.service.SupplierService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/supplier")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SupplierController {

    private final SupplierService supplierService;

    @GetMapping("/orders")
    public ResponseEntity<List<PurchaseOrder>> all() {
        return ResponseEntity.ok(supplierService.getAll());
    }

    @PostMapping("/orders")
    public ResponseEntity<PurchaseOrder> create(@RequestBody PurchaseOrder order) {
        return ResponseEntity.ok(supplierService.createManual(order));
    }

    @PutMapping("/orders/{id}/status")
    public ResponseEntity<PurchaseOrder> updateStatus(@PathVariable Long id,
                                                       @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(supplierService.updateStatus(
            id, PurchaseOrder.OrderStatus.valueOf(body.get("status").toUpperCase())
        ));
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "supplier-service",
            "agent", "Supplier Saga Agent ACTIVE"
        ));
    }
}
