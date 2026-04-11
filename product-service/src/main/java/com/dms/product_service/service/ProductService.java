package com.dms.product_service.service;

import com.dms.product_service.client.AuditClient;
import com.dms.product_service.client.InventoryClient;
import com.dms.product_service.dto.AuditEventDto;
import com.dms.product_service.entity.Product;
import com.dms.product_service.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final InventoryClient inventoryClient;
    private final AuditClient auditClient;

    public List<Product> findAll() {
        List<Product> products = productRepository.findAll();
        products.forEach(this::populateStockFromInventory);
        return products;
    }

    public List<Product> findActive() {
        List<Product> products = productRepository.findByActiveTrue();
        products.forEach(this::populateStockFromInventory);
        return products;
    }

    public Product getById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id " + id));
        populateStockFromInventory(product);
        return product;
    }

    @Transactional
    public Product create(Product product) {
        if (product.getActive() == null) {
            product.setActive(true);
        }
        Integer requestedStock = product.getStock();
        Product saved = productRepository.save(product);
        try {
            if (requestedStock != null) {
                inventoryClient.setStock(saved.getId(), requestedStock, "DEFAULT");
            }
            saved.setStock(requestedStock != null ? requestedStock : 0);
        } catch (RuntimeException ex) {
            throw new RuntimeException("Product created but failed to update inventory: " + ex.getMessage(), ex);
        }
        auditClient.logEvent(new AuditEventDto(
                null,
                "PRODUCT_CREATED",
                "SYSTEM",
                "PRODUCT",
                String.valueOf(saved.getId()),
                "Product created: " + saved.getName(),
                LocalDateTime.now()
        ));
        return saved;
    }

    @Transactional
    public Product update(Long id, Product incoming) {
        Product existing = getById(id);
        existing.setName(incoming.getName());
        existing.setDescription(incoming.getDescription());
        existing.setPrice(incoming.getPrice());
        existing.setActive(incoming.getActive());
        Product saved = productRepository.save(existing);
        Integer requestedStock = incoming.getStock();
        if (requestedStock != null) {
            inventoryClient.setStock(saved.getId(), requestedStock, "DEFAULT");
            saved.setStock(requestedStock);
        }
        auditClient.logEvent(new AuditEventDto(
                null,
                "PRODUCT_UPDATED",
                "SYSTEM",
                "PRODUCT",
                String.valueOf(saved.getId()),
                "Product updated: " + saved.getName(),
                LocalDateTime.now()
        ));
        return saved;
    }

    @Transactional
    public void delete(Long id) {
        Product product = getById(id);
        auditClient.logEvent(new AuditEventDto(
                null,
                "PRODUCT_DELETED",
                "SYSTEM",
                "PRODUCT",
                String.valueOf(product.getId()),
                "Product deleted: " + product.getName(),
                LocalDateTime.now()
        ));
        productRepository.delete(product);
    }

    private void populateStockFromInventory(Product product) {
        try {
            product.setStock(inventoryClient.getStock(product.getId()));
        } catch (RuntimeException ex) {
            product.setStock(0);
        }
    }
}

