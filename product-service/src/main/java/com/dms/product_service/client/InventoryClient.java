package com.dms.product_service.client;

import com.dms.product_service.dto.InventoryItemDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

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
}
