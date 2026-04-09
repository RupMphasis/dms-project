# Distributor Service Architecture

## Overview

`distributor-service` is the microservice responsible for managing distributor records used by the system.

### Purpose
- Stores and serves distributor details
- Supports CRUD operations for distributors
- Acts as the authoritative service for distributor data
- Used by `user-service` when distributor users register

---

## Technology Stack

- Java + Spring Boot
- Spring Data JPA for database access
- Spring Web for REST API endpoints
- Jakarta Persistence for entity mapping
- Standard Spring component wiring

---

## Directory and Component Flow

### `src/main/java/com/dms/distributor`
This is the main Java package of `distributor-service`.

#### `controller/`
- `DistributorController.java`
  - Exposes REST endpoints under `/distributors`
  - Implements CRUD operations:
    - `POST /distributors` to create a distributor
    - `GET /distributors` to list all distributors
    - `GET /distributors/{id}` to fetch one distributor
    - `PUT /distributors/{id}` to update distributor details
    - `DELETE /distributors/{id}` to remove a distributor

#### `service/`
- `DistributorService.java`
  - Contains business logic for distributor operations
  - Uses `DistributorRepository` to perform persistence
  - Handles:
    - save
    - list all
    - fetch by id
    - update
    - delete
  - Throws runtime exception if the requested distributor is not found

#### `repository/`
- `DistributorRepository.java`
  - Extends `JpaRepository<Distributor, Long>`
  - Provides default database operations without custom methods

#### `entity/`
- `Distributor.java`
  - JPA entity mapped to `distributors` table
  - Fields:
    - `id`
    - `name`
    - `email`
    - `city`
    - `contact`
  - Uses `@Entity` and `@Table` annotations for ORM mapping

---

## Request Flow

### Create Distributor
1. A client sends `POST /distributors` with distributor JSON
2. `DistributorController.createDistributor()` receives the request
3. It delegates to `DistributorService.saveDistributor()`
4. `DistributorRepository.save()` persists the distributor record
5. The saved distributor object is returned with generated `id`

### Read Distributor
1. `GET /distributors` returns all distributors
2. `GET /distributors/{id}` returns one distributor by ID
3. `DistributorService.getDistributorById()` throws if ID is missing

### Update Distributor
1. `PUT /distributors/{id}` receives updated fields
2. Controller delegates to `DistributorService.updateDistributor()`
3. Service loads existing entity, applies changes, saves it back

### Delete Distributor
1. `DELETE /distributors/{id}` calls `DistributorService.deleteDistributor()`
2. Repository removes the entity from the database

---

## Summary

`distributor-service` is a simple CRUD microservice for distributor entities. It exposes REST endpoints, uses Spring Data JPA for persistence, and is called by `user-service` when distributor user accounts are created. The architecture is clean: controller → service → repository → database.
