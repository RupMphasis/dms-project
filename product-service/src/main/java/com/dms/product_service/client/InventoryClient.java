package com.dms.product_service.client;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class InventoryClient {

    private final RestTemplate restTemplate;

    @Value("${inventory.service.url:http://localhost:8082}")
    private String inventoryServiceUrl;

    public Integer getStock(Long productId) {
        String url = String.format("%s/api/inventory/product/%d", inventoryServiceUrl, productId);
        try {
            ResponseEntity<InventoryResponse> response = restTemplate.getForEntity(url, InventoryResponse.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new RuntimeException("Failed to fetch inventory for productId " + productId);
            }
            return response.getBody().getQuantity();
        } catch (HttpStatusCodeException ex) {
            if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new RuntimeException("Inventory item not found for productId: " + productId, ex);
            }
            throw new RuntimeException("Failed to fetch inventory for productId " + productId + ": " + ex.getMessage(), ex);
        }
    }

    public void createInventory(Long productId, Integer quantity, String location) {
        InventoryRequest request = new InventoryRequest();
        request.setProductId(productId);
        request.setQuantity(quantity != null ? quantity : 0);
        request.setLocation(location);
        String url = inventoryServiceUrl + "/api/inventory";
        ResponseEntity<Void> response = restTemplate.postForEntity(url, request, Void.class);
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Failed to create inventory for productId " + productId);
        }
    }

    public void adjustStock(Long productId, int delta) {
        String url = String.format("%s/api/inventory/%d/adjust?delta=%d", inventoryServiceUrl, productId, delta);
        ResponseEntity<Void> response = restTemplate.postForEntity(url, null, Void.class);
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Failed to adjust inventory for productId " + productId);
        }
    }

    public void setStock(Long productId, Integer quantity, String location) {
        if (quantity == null) {
            quantity = 0;
        }
        try {
            Integer current = getStock(productId);
            int delta = quantity - current;
            if (delta != 0) {
                adjustStock(productId, delta);
            }
        } catch (RuntimeException ex) {
            if (ex.getMessage() != null && ex.getMessage().contains("Inventory item not found")) {
                createInventory(productId, quantity, location);
                return;
            }
            throw ex;
        }
    }

    private static class InventoryResponse {
        private Integer quantity;

        public Integer getQuantity() {
            return quantity;
        }

        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }
    }

    private static class InventoryRequest {
        private Long productId;
        private Integer quantity;
        private String location;

        public Long getProductId() {
            return productId;
        }

        public void setProductId(Long productId) {
            this.productId = productId;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }

        public String getLocation() {
            return location;
        }

        public void setLocation(String location) {
            this.location = location;
        }
    }
}