package com.nexops.inventory.controller;

import com.nexops.inventory.entity.Product;
import com.nexops.inventory.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping("/products")
    public ResponseEntity<List<Product>> all() {
        return ResponseEntity.ok(inventoryService.getAllProducts());
    }

    @GetMapping("/products/{id}")
    public ResponseEntity<Product> one(@PathVariable Long id) {
        return ResponseEntity.ok(inventoryService.getProduct(id));
    }

    @PostMapping("/products")
    public ResponseEntity<Product> create(@RequestBody Product product) {
        return ResponseEntity.ok(inventoryService.createProduct(product));
    }

    @PutMapping("/products/{id}/stock")
    public ResponseEntity<Product> updateStock(@PathVariable Long id,
                                                @RequestBody Map<String, Integer> body) {
        return ResponseEntity.ok(inventoryService.updateStock(id, body.get("quantityChange")));
    }

    @PutMapping("/products/{id}/price")
    public ResponseEntity<Product> updatePrice(@PathVariable Long id,
                                               @RequestBody Map<String, BigDecimal> body) {
        return ResponseEntity.ok(inventoryService.updatePrice(id, body.get("price")));
    }

    @PutMapping("/products/{id}")
    public ResponseEntity<Product> update(@PathVariable Long id, @RequestBody Product product) {
        return ResponseEntity.ok(inventoryService.updateProduct(id, product));
    }

    @DeleteMapping("/products/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        inventoryService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/products/low-stock")
    public ResponseEntity<List<Product>> lowStock() {
        return ResponseEntity.ok(inventoryService.getLowStockProducts());
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "inventory-service"));
    }
}
