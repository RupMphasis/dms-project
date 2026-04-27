package com.dms.distributor.service;

import com.dms.distributor.entity.Distributor;
import com.dms.distributor.repository.DistributorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class DistributorService {

    @Autowired
    private DistributorRepository distributorRepository;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${order.service.url:http://localhost:8083}")
    private String orderServiceUrl;

    @Value("${inventory.service.url:http://localhost:8085}")
    private String inventoryServiceUrl;

    // CREATE
    public Distributor saveDistributor(Distributor distributor) {
        return distributorRepository.save(distributor);
    }

    // READ ALL
    public List<Distributor> getAllDistributors() {
        return distributorRepository.findAll();
    }

    // READ BY ID
    public Distributor getDistributorById(Long id) {
        return distributorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Distributor not found"));
    }

    // UPDATE
    public Distributor updateDistributor(Long id, Distributor updatedDistributor) {
        Distributor existingDistributor = getDistributorById(id);

        if (existingDistributor != null) {
            existingDistributor.setName(updatedDistributor.getName());
            existingDistributor.setEmail(updatedDistributor.getEmail());
            existingDistributor.setCity(updatedDistributor.getCity());
            existingDistributor.setContact(updatedDistributor.getContact());

            return distributorRepository.save(existingDistributor);
        }
        return null;
    }

    // DELETE — guarded with cross-service dependency checks for ACID integrity
    public void deleteDistributor(Long id) {
        Distributor distributor = getDistributorById(id);

        // ── 1. Check active orders ────────────────────────────────────────────
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> orderCountRes = restTemplate.getForObject(
                    orderServiceUrl + "/api/orders/count/distributor/" + id,
                    Map.class);
            long activeOrders = orderCountRes != null && orderCountRes.get("count") != null
                    ? ((Number) orderCountRes.get("count")).longValue() : 0L;
            if (activeOrders > 0) {
                throw new IllegalStateException(
                        "Cannot delete distributor '" + distributor.getName() + "': "
                        + activeOrders + " active order(s) still reference this distributor. "
                        + "Cancel or reject them first.");
            }
        } catch (IllegalStateException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException(
                    "Cannot verify active orders for distributor " + id
                    + " — order-service may be unavailable. Please try again later. (" + ex.getMessage() + ")");
        }

        // ── 2. Check distributor inventory records ────────────────────────────
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> invCountRes = restTemplate.getForObject(
                    inventoryServiceUrl + "/api/distributor-inventory/count/distributor/" + id,
                    Map.class);
            long invCount = invCountRes != null && invCountRes.get("count") != null
                    ? ((Number) invCountRes.get("count")).longValue() : 0L;
            if (invCount > 0) {
                throw new IllegalStateException(
                        "Cannot delete distributor '" + distributor.getName() + "': "
                        + invCount + " distributor inventory record(s) still exist. "
                        + "Clear all distributor inventory first.");
            }
        } catch (IllegalStateException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException(
                    "Cannot verify distributor inventory for distributor " + id
                    + " — inventory-service may be unavailable. Please try again later. (" + ex.getMessage() + ")");
        }

        distributorRepository.deleteById(id);
    }
}

