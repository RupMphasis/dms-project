package com.dms.inventory_service.service;

import com.dms.inventory_service.entity.DistributorInventoryItem;
import com.dms.inventory_service.exception.InventoryBadRequestException;
import com.dms.inventory_service.repository.DistributorInventoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DistributorInventoryService {

    private final DistributorInventoryRepository repository;

    public List<DistributorInventoryItem> findAll() {
        return repository.findAll();
    }

    public List<DistributorInventoryItem> findByDistributorId(Long distributorId) {
        return repository.findByDistributorId(distributorId);
    }

    public DistributorInventoryItem getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Distributor inventory item not found with id: " + id));
    }

    public DistributorInventoryItem getByProductIdAndDistributorId(Long productId, Long distributorId) {
        return repository.findByProductIdAndDistributorId(productId, distributorId)
                .orElseThrow(() -> new InventoryBadRequestException("Distributor inventory item not found for productId: " + productId + " and distributorId: " + distributorId));
    }

    @Transactional
    public DistributorInventoryItem create(DistributorInventoryItem item) {
        item.setLastUpdated(LocalDateTime.now());
        if (item.getTotalPurchased() == null) item.setTotalPurchased(0);
        if (item.getTotalSold() == null) item.setTotalSold(0);
        return repository.save(item);
    }

    @Transactional
    public DistributorInventoryItem update(Long id, DistributorInventoryItem item) {
        DistributorInventoryItem existing = getById(id);
        existing.setQuantity(item.getQuantity());
        if (item.getTotalPurchased() != null) existing.setTotalPurchased(item.getTotalPurchased());
        if (item.getTotalSold() != null) existing.setTotalSold(item.getTotalSold());
        existing.setLastUpdated(LocalDateTime.now());
        return repository.save(existing);
    }

    @Transactional
    public void delete(Long id) {
        DistributorInventoryItem existing = getById(id);
        repository.delete(existing);
    }

    @Transactional
    public void adjustStock(Long productId, Long distributorId, int delta, String transactionType) {
        DistributorInventoryItem item = repository.findByProductIdAndDistributorId(productId, distributorId).orElse(null);
        if (item == null) {
            item = DistributorInventoryItem.builder()
                    .productId(productId)
                    .distributorId(distributorId)
                    .quantity(0)
                    .totalPurchased(0)
                    .totalSold(0)
                    .build();
        }

        int newQty = item.getQuantity() + delta;
        if (newQty < 0) {
            throw new InventoryBadRequestException("Insufficient distributor inventory for productId: " + productId);
        }

        item.setQuantity(newQty);
        
        if ("PURCHASE".equalsIgnoreCase(transactionType) && delta > 0) {
            item.setTotalPurchased(item.getTotalPurchased() + delta);
        } else if ("MANUAL_SALE".equalsIgnoreCase(transactionType) && delta < 0) {
            item.setTotalSold(item.getTotalSold() + Math.abs(delta));
        }

        item.setLastUpdated(LocalDateTime.now());
        repository.save(item);
    }
}
