package com.dms.distributor.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "distributors")
public class Distributor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String city;
    private String contact;

    // Constructors
    public Distributor() {
    }

    public Distributor(Long id, String name, String city, String contact) {
        this.id = id;
        this.name = name;
        this.city = city;
        this.contact = contact;
    }

    // Getters & Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getContact() {
        return contact;
    }

    public void setContact(String contact) {
        this.contact = contact;
    }
}
