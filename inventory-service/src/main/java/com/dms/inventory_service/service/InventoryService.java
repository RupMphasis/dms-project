package com.dms.inventory_service.service;

import com.dms.inventory_service.entity.InventoryItem;
import com.dms.inventory_service.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;

    public List<InventoryItem> findAll() {
        return inventoryRepository.findAll();
    }

    public InventoryItem getById(Long id) {
        return inventoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Inventory item not found with id: " + id));
    }

    public InventoryItem getByProductId(Long productId) {
        return inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new RuntimeException("Inventory item not found for productId: " + productId));
    }

    @Transactional
    public InventoryItem create(InventoryItem item) {
        return inventoryRepository.save(item);
    }

    @Transactional
    public InventoryItem update(Long id, InventoryItem item) {
        InventoryItem existing = getById(id);
        existing.setProductId(item.getProductId());
        existing.setQuantity(item.getQuantity());
        existing.setLocation(item.getLocation());
        return inventoryRepository.save(existing);
    }

    @Transactional
    public void delete(Long id) {
        InventoryItem existing = getById(id);
        inventoryRepository.delete(existing);
    }

    @Transactional
    public void adjustStock(Long productId, int delta) {
        InventoryItem item = getByProductId(productId);
        int newQty = item.getQuantity() + delta;
        if (newQty < 0) {
            throw new RuntimeException("Insufficient inventory for productId: " + productId);
        }
        item.setQuantity(newQty);
        inventoryRepository.save(item);
    }
}