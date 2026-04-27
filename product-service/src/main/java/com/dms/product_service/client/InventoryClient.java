package com.dms.product_service.client;

import com.dms.product_service.dto.InventoryItemDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
public class InventoryClient {

    private final RestTemplate restTemplate;
    private final String inventoryServiceUrl;

    public InventoryClient(RestTemplate restTemplate,
                           @Value("${inventory.service.url:http://localhost:8085}") String inventoryServiceUrl) {
        this.restTemplate = restTemplate;
        this.inventoryServiceUrl = inventoryServiceUrl;
    }

    public Integer getStock(Long productId) {
        InventoryItemDto item = restTemplate.getForObject(
                inventoryServiceUrl + "/api/inventory/product/{productId}",
                InventoryItemDto.class,
                productId);
        return item == null ? 0 : item.getQuantity();
    }

    public void setStock(Long productId, Integer quantity, String location) {
        try {
            InventoryItemDto existing = restTemplate.getForObject(
                    inventoryServiceUrl + "/api/inventory/product/{productId}",
                    InventoryItemDto.class,
                    productId);
            if (existing != null && existing.getId() != null) {
                existing.setQuantity(quantity != null ? quantity : 0);
                existing.setLocation(location);
                restTemplate.put(inventoryServiceUrl + "/api/inventory/{id}", existing, existing.getId());
                return;
            }
        } catch (Exception ignored) {
            // create new if not found
        }

        InventoryItemDto newItem = new InventoryItemDto();
        newItem.setProductId(productId);
        newItem.setQuantity(quantity != null ? quantity : 0);
        newItem.setLocation(location);
        restTemplate.postForObject(inventoryServiceUrl + "/api/inventory", newItem, InventoryItemDto.class);
    }

    public void adjustStock(Long productId, Integer delta) {
        String url = String.format("%s/api/inventory/%d/adjust?delta=%d", inventoryServiceUrl, productId, delta != null ? delta : 0);
        restTemplate.postForEntity(url, null, Void.class);
    }

    /**
     * Checks if a central inventory row exists for the given product.
     * Used before deleting a product to enforce ACID integrity.
     */
    @SuppressWarnings("unchecked")
    public boolean existsInventoryForProduct(Long productId) {
        try {
            Map<String, Boolean> response = restTemplate.getForObject(
                    inventoryServiceUrl + "/api/inventory/exists/product/" + productId,
                    Map.class);
            return response != null && Boolean.TRUE.equals(response.get("exists"));
        } catch (Exception ex) {
            throw new IllegalStateException("Cannot verify inventory for product " + productId
                    + " — inventory-service may be unavailable. Please try again later.", ex);
        }
    }

    /**
     * Returns the count of distributor-inventory rows referencing a product.
     * Used before deleting a product to enforce ACID integrity.
     */
    @SuppressWarnings("unchecked")
    public long countDistributorInventoryByProduct(Long productId) {
        try {
            Map<String, Object> response = restTemplate.getForObject(
                    inventoryServiceUrl + "/api/distributor-inventory/count/product/" + productId,
                    Map.class);
            return response != null && response.get("count") != null ? ((Number) response.get("count")).longValue() : 0L;
        } catch (Exception ex) {
            throw new IllegalStateException("Cannot verify distributor inventory for product " + productId
                    + " — inventory-service may be unavailable. Please try again later.", ex);
        }
    }
}
