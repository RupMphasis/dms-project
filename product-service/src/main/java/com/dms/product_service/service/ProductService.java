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
        return productRepository.findAll();
    }

    public List<Product> findActive() {
        return productRepository.findByActiveTrue();
    }

    public Product getById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id " + id));
    }

    public Integer getCurrentStock(Long productId) {
        return inventoryClient.getStock(productId);
    }

    @Transactional
    public Product create(Product product) {
        if (product.getActive() == null) {
            product.setActive(true);
        }
        Product saved = productRepository.save(product);
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
        existing.setStock(incoming.getStock());
        existing.setActive(incoming.getActive());
        Product saved = productRepository.save(existing);
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
}
