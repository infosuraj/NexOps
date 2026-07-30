package com.nexops.inventory.service;

import com.nexops.inventory.entity.Product;
import com.nexops.inventory.kafka.InventoryEventPublisher;
import com.nexops.inventory.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {

    private final ProductRepository productRepository;
    private final InventoryEventPublisher eventPublisher;

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    public Product getProduct(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found: " + id));
    }

    @Transactional
    public Product createProduct(Product product) {
        if (productRepository.existsByNameIgnoreCase(product.getName())) {
            throw new RuntimeException("A product named \"" + product.getName() + "\" already exists");
        }
        product.setCurrentPrice(product.getBasePrice());
        Product saved = productRepository.save(product);
        log.info("New product created: {} (id={})", saved.getName(), saved.getId());
        return saved;
    }

    @Transactional
    public Product updateStock(Long productId, int quantityChange) {
        Product p = getProduct(productId);
        int oldQty = p.getStockQuantity();
        int newQty = oldQty + quantityChange;

        if (newQty < 0) {
            throw new RuntimeException("Not enough stock for: " + p.getName());
        }

        p.setStockQuantity(newQty);
        Product updated = productRepository.save(p);

        // always publish stock change
        eventPublisher.publishStockUpdate(productId, p.getName(), oldQty, newQty);

        // check if we've hit the low stock threshold
        if (newQty <= p.getLowStockThreshold()) {
            eventPublisher.publishLowStockAlert(productId, p.getName(), newQty, p.getLowStockThreshold());
        }

        // if this was a purchase (negative change), send demand signal
        if (quantityChange < 0) {
            eventPublisher.publishDemandEvent(productId, p.getName());
        }

        return updated;
    }

    @Transactional
    public Product updatePrice(Long productId, BigDecimal newPrice) {
        Product p = getProduct(productId);
        p.setCurrentPrice(newPrice);
        return productRepository.save(p);
    }

    @Transactional
    public Product updateProduct(Long id, Product updated) {
        Product p = getProduct(id);
        p.setName(updated.getName());
        p.setCategory(updated.getCategory());
        p.setStockQuantity(updated.getStockQuantity());
        p.setLowStockThreshold(updated.getLowStockThreshold());
        p.setBasePrice(updated.getBasePrice());
        return productRepository.save(p);
    }

    @Transactional
    public void deleteProduct(Long id) {
        productRepository.deleteById(id);
        log.info("Product {} deleted", id);
    }

    public List<Product> getLowStockProducts() {
        return productRepository.findLowStockProducts();
    }
}
