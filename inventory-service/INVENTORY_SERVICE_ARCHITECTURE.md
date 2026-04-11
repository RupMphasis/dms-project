# Inventory Service Architecture

## Overview

`inventory-service` manages inventory stock records for products used by orders and products in the system.

### Purpose
- Stores inventory items linked to products
- Adjusts quantity when orders are placed or canceled
- Provides inventory lookup by product or id
- Protects inventory from negative stock

---

## Technology Stack

- Java 17 + Spring Boot
- Spring Web for REST APIs
- Spring Data JPA with Hibernate for persistence
- Spring Security for endpoint protection
- JSON Web Tokens (JJWT) for authentication
- Spring Actuator for monitoring
- Spring Cloud Eureka client for service registration/discovery
- Springdoc OpenAPI for Swagger documentation
- Lombok for boilerplate reduction
- MySQL Connector/J for database connectivity
- Jakarta Persistence for entity mapping
- Spring Transaction management

---

## Directory and Component Flow

### `src/main/java/com/dms/inventory_service`
This is the main Java package of `inventory-service`.

#### `controller/`
- `InventoryController.java`
  - Exposes REST API under `/api/inventory`
  - Endpoints:
    - `GET /api/inventory` — list all inventory items
    - `GET /api/inventory/{id}` — get inventory item by id
    - `GET /api/inventory/product/{productId}` — get inventory by product id
    - `POST /api/inventory` — create an inventory item
    - `PUT /api/inventory/{id}` — update inventory item fields
    - `DELETE /api/inventory/{id}` — delete inventory item
    - `POST /api/inventory/{productId}/adjust?delta=X` — adjust stock quantity by delta

#### `service/`
- `InventoryService.java`
  - Business logic for inventory operations
  - Uses `InventoryRepository` for database access
  - Methods:
    - `findAll()` — return all inventory items
    - `getById(id)` — get item by inventory id
    - `getByProductId(productId)` — get item by product id
    - `create(item)` — save a new inventory item
    - `update(id, item)` — update existing inventory item
    - `delete(id)` — remove inventory item
    - `adjustStock(productId, delta)` — add or subtract quantity
  - Inventory rules:
    - Throws an exception when inventory item is missing
    - Prevents negative quantity and throws `InventoryBadRequestException`

#### `repository/`
- `InventoryRepository.java`
  - Extends `JpaRepository<InventoryItem, Long>`
  - Adds:
    - `findByProductId(Long productId)`
    - `findByLocation(String location)`

#### `entity/`
- `InventoryItem.java`
  - JPA entity mapped to `inventory` table
  - Fields:
    - `id`
    - `productId`
    - `quantity`
    - `location`
  - Uses Lombok annotations for getters, setters, constructors, and builder

---

## Request Flow

### Lookup Inventory
1. Client sends `GET /api/inventory/product/{productId}` or `GET /api/inventory/{id}`
2. `InventoryController` forwards the request to `InventoryService`
3. Service loads the inventory item and returns it

### Create or Update Inventory
1. Client sends `POST /api/inventory` or `PUT /api/inventory/{id}`
2. `InventoryService` saves or updates the inventory record via repository
3. Returns the saved item

### Adjust Stock
1. Client sends `POST /api/inventory/{productId}/adjust?delta=X`
2. `InventoryController.adjustStock()` calls `InventoryService.adjustStock(productId, delta)`
3. Service checks existing item by productId
4. Calculates `newQty = currentQty + delta`
5. If `newQty < 0`, throws an exception
6. Otherwise saves the updated quantity

---

## Integration Points

- `order-service`
  - Calls `inventory-service` to decrement stock when orders are approved
  - Restores stock when orders are canceled or deleted

- `product-service`
  - May use inventory data to populate product stock values

---

## Summary

`inventory-service` is the dedicated stock management microservice. It provides inventory CRUD operations and stock adjustment logic to ensure product quantities remain consistent. The architecture is:
- controller → service → repository
- with clear validation for stock adjustments

This file is ready for PDF conversion.
