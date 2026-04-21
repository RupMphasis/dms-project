package com.dms.product_service.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "products")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 100)
    private String vehicleType;

    @Column(nullable = false, length = 50)
    private String size;

    @Column(nullable = false, length = 500)
    private String description;

    @Column(nullable = false)
    private Double price;

    @Transient
    private Integer stock;

    private Integer productionCapacityPerDay;

    @Column(nullable = false)
    private Boolean active;
}
