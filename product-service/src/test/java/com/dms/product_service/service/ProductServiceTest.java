package com.dms.product_service.service;

import com.dms.product_service.client.AuditClient;
import com.dms.product_service.client.InventoryClient;
import com.dms.product_service.entity.Product;
import com.dms.product_service.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private InventoryClient inventoryClient;

    @Mock
    private AuditClient auditClient;

    @InjectMocks
    private ProductService productService;

    private Product product;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);

        product = Product.builder()
                .id(1L)
                .name("Tyre")
                .vehicleType("Car")
                .size("Medium")
                .description("Test product")
                .price(100.0)
                .active(true)
                .build();
    }

    @Test
    void testFindAll() {
        when(productRepository.findAll()).thenReturn(List.of(product));
        when(inventoryClient.getStock(1L)).thenReturn(10);

        List<Product> result = productService.findAll();

        assertEquals(1, result.size());
        assertEquals(10, result.get(0).getStock());
    }

    @Test
    void testFindActive() {
        when(productRepository.findByActiveTrue()).thenReturn(List.of(product));
        when(inventoryClient.getStock(1L)).thenReturn(5);

        List<Product> result = productService.findActive();

        assertEquals(5, result.get(0).getStock());
    }

    @Test
    void testGetById_Success() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(inventoryClient.getStock(1L)).thenReturn(15);

        Product result = productService.getById(1L);

        assertEquals(15, result.getStock());
    }

    @Test
    void testGetById_NotFound() {
        when(productRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> productService.getById(1L));
    }

    @Test
    void testCreate_ProductWithStock() {
        product.setStock(20);

        when(productRepository.save(any(Product.class))).thenReturn(product);

        Product result = productService.create(product);

        verify(inventoryClient).setStock(1L, 20, "DEFAULT");
        verify(auditClient).logEvent(any());

        assertEquals(20, result.getStock());
    }

    @Test
    void testCreate_InventoryFailure() {
        product.setStock(20);

        when(productRepository.save(any(Product.class))).thenReturn(product);
        doThrow(new RuntimeException("Inventory down"))
                .when(inventoryClient).setStock(any(), any(), any());

        assertThrows(RuntimeException.class, () -> productService.create(product));
    }

    @Test
    void testUpdate() {
        Product updated = Product.builder()
                .name("Updated")
                .description("Updated desc")
                .price(200.0)
                .active(true)
                .stock(30)
                .build();

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        Product result = productService.update(1L, updated);

        verify(inventoryClient).setStock(1L, 30, "DEFAULT");
        verify(auditClient).logEvent(any());

        assertEquals("Updated", result.getName());
    }

    @Test
    void testDelete() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        productService.delete(1L);

        verify(productRepository).delete(product);
        verify(auditClient).logEvent(any());
    }

    @Test
    void testPopulateStock_ExceptionHandled() {
        when(productRepository.findAll()).thenReturn(List.of(product));
        when(inventoryClient.getStock(1L)).thenThrow(new RuntimeException());

        List<Product> result = productService.findAll();

        assertEquals(0, result.get(0).getStock());
    }
}