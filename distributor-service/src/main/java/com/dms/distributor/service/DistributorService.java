package com.dms.distributor.service;

import com.dms.distributor.entity.Distributor;
import com.dms.distributor.repository.DistributorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DistributorService {

    @Autowired
    private DistributorRepository distributorRepository;

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

    // DELETE
    public void deleteDistributor(Long id) {
        distributorRepository.deleteById(id);
    }
}
