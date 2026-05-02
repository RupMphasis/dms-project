package com.dms.product_service.service;

import com.dms.product_service.client.AuditClient;
import com.dms.product_service.client.InventoryClient;
import com.dms.product_service.client.OrderClient;
import com.dms.product_service.dto.AuditEventDto;
import com.dms.product_service.entity.Product;
import com.dms.product_service.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final InventoryClient inventoryClient;
    private final AuditClient auditClient;
    private final OrderClient orderClient;
    private final RestTemplate restTemplate;

    @Value("${order.service.url:http://localhost:8083}")
    private String orderServiceUrl;

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
        try {
            recalculateProductOrders(saved.getId());
        } catch (Exception ignored) {
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
        existing.setVehicleType(incoming.getVehicleType());
        existing.setSize(incoming.getSize());
        existing.setDescription(incoming.getDescription());
        existing.setPrice(incoming.getPrice());
        existing.setProductionCapacityPerDay(incoming.getProductionCapacityPerDay());
        existing.setActive(incoming.getActive());
        Product saved = productRepository.save(existing);
        Integer requestedStock = incoming.getStock();
        if (requestedStock != null) {
            inventoryClient.setStock(saved.getId(), requestedStock, "DEFAULT");
            saved.setStock(requestedStock);
        }
        try {
            recalculateProductOrders(saved.getId());
        } catch (Exception ignored) {
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

        // ── 1. Check active orders ─────────────────────────────────────────────
        long activeOrders = orderClient.countActiveOrdersByProduct(id);
        if (activeOrders > 0) {
            throw new IllegalStateException(
                    "Cannot delete product '" + product.getName() + "': it still has "
                    + activeOrders + " active order(s). Cancel or reject them first.");
        }

        // ── 2. Check central inventory row ────────────────────────────────────
        boolean hasInventory = inventoryClient.existsInventoryForProduct(id);
        if (hasInventory) {
            throw new IllegalStateException(
                    "Cannot delete product '" + product.getName() + "': a central inventory record still exists. "
                    + "Delete the inventory entry first.");
        }

        // ── 3. Check distributor inventory rows ───────────────────────────────
        long distInvCount = inventoryClient.countDistributorInventoryByProduct(id);
        if (distInvCount > 0) {
            throw new IllegalStateException(
                    "Cannot delete product '" + product.getName() + "': "
                    + distInvCount + " distributor inventory record(s) still reference it. "
                    + "Clear all distributor inventory for this product first.");
        }

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

    private void recalculateProductOrders(Long productId) {
        String url = orderServiceUrl + "/api/orders/product/" + productId + "/recalculate";
        restTemplate.postForEntity(url, null, Void.class);
    }
}


