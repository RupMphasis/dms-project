package com.dms.product_service.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * HTTP client for querying the order-service.
 * Used to check active order dependencies before deleting a product.
 */
@Component
public class OrderClient {

    private final RestTemplate restTemplate;
    private final String orderServiceUrl;

    public OrderClient(RestTemplate restTemplate,
                       @Value("${order.service.url:http://localhost:8083}") String orderServiceUrl) {
        this.restTemplate = restTemplate;
        this.orderServiceUrl = orderServiceUrl;
    }

    /**
     * Returns the number of active (non-terminal) orders referencing a product.
     */
    @SuppressWarnings("unchecked")
    public long countActiveOrdersByProduct(Long productId) {
        try {
            Map<String, Object> response = restTemplate.getForObject(
                    orderServiceUrl + "/api/orders/count/product/" + productId,
                    Map.class);
            return response != null && response.get("count") != null ? ((Number) response.get("count")).longValue() : 0L;
        } catch (Exception ex) {
            throw new IllegalStateException("Cannot verify active orders for product " + productId
                    + " — order-service may be unavailable. Please try again later.", ex);
        }
    }
}
