package com.dms.product_service.client;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class InventoryClient {

    private final RestTemplate restTemplate;

    @Value("${inventory.service.url:http://localhost:8082}")
    private String inventoryServiceUrl;

    public Integer getStock(Long productId) {
        String url = String.format("%s/api/inventory/product/%d", inventoryServiceUrl, productId);
        ResponseEntity<InventoryResponse> response = restTemplate.getForEntity(url, InventoryResponse.class);
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new RuntimeException("Failed to fetch inventory for productId " + productId);
        }
        return response.getBody().getQuantity();
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
}