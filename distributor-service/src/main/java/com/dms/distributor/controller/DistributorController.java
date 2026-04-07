package com.dms.distributor.controller;

import com.dms.distributor.entity.Distributor;
import com.dms.distributor.service.DistributorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/distributors")
public class DistributorController {

    @Autowired
    private DistributorService distributorService;

    // CREATE
    @PostMapping
    public Distributor createDistributor(@RequestBody Distributor distributor) {
        return distributorService.saveDistributor(distributor);
    }

    // READ ALL
    @GetMapping
    public List<Distributor> getAllDistributors() {
        return distributorService.getAllDistributors();
    }

    // READ BY ID
    @GetMapping("/{id}")
    public Distributor getDistributorById(@PathVariable Long id) {
        return distributorService.getDistributorById(id);
    }

    // UPDATE
    @PutMapping("/{id}")
    public Distributor updateDistributor(@PathVariable Long id,
                                         @RequestBody Distributor distributor) {
        return distributorService.updateDistributor(id, distributor);
    }

    // DELETE
    @DeleteMapping("/{id}")
    public String deleteDistributor(@PathVariable Long id) {
        distributorService.deleteDistributor(id);
        return "Distributor deleted successfully";
    }
}
