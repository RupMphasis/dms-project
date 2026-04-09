# Product Service Architecture

## Overview

`product-service` manages product data and inventory synchronization in the system.

### Purpose
- Stores product information
- Supports product CRUD operations
- Returns active products and product details
- Syncs stock information with `inventory-service`
- Sends audit events to `audit-service`

---

## Technology Stack

- Java + Spring Boot
- Spring Data JPA for persistence
- Spring Web for REST endpoints
- Spring Transaction management
- Lombok for boilerplate reduction
- Jakarta Persistence for entity mapping
- `RestTemplate`-based clients for service calls (inventory + audit)

---

## Directory and Component Flow

### `src/main/java/com/dms/product_service`
This is the main Java package of `product-service`.

#### `controller/`
- `ProductController.java`
  - REST API endpoints under `/api/products`
  - Endpoints:
    - `GET /api/products` - list all products
    - `GET /api/products/active` - list active products only
    - `GET /api/products/{id}` - fetch product by id
    - `POST /api/products` - create a new product
    - `PUT /api/products/{id}` - update an existing product
    - `DELETE /api/products/{id}` - delete a product

#### `service/`
- `ProductService.java`
  - Business logic and product lifecycle management
  - Uses `ProductRepository` for database operations
  - Uses `InventoryClient` to read or update stock from inventory service
  - Uses `AuditClient` to log product events
  - Methods:
    - `findAll()` - returns all products and populates stock
    - `findActive()` - returns only active products with stock
    - `getById(id)` - returns a product with current stock
    - `create(product)` - saves a new product and initializes stock in inventory
    - `update(id, product)` - updates product details and inventory stock
    - `delete(id)` - removes a product and logs deletion
  - Internal flow:
    - `populateStockFromInventory(product)` calls inventory client and sets product stock
    - If inventory lookup fails, stock defaults to `0`

#### `repository/`
- `ProductRepository.java`
  - Extends `JpaRepository<Product, Long>`
  - Adds `findByActiveTrue()` for active-product filtering

#### `entity/`
- `Product.java`
  - JPA entity mapped to `products` table
  - Fields:
    - `id`
    - `name`
    - `vehicleType`
    - `size`
    - `description`
    - `price`
    - `stock`
    - `active`
  - Uses Lombok annotations for getters, setters, constructors, and builder.

---

## Service Interactions

- `inventory-service`
  - `ProductService` calls `InventoryClient.getStock(productId)` to populate stock values
  - On create/update, it calls `InventoryClient.setStock(productId, stock, "DEFAULT")`

- `audit-service`
  - After create, update, or delete operations, `ProductService` logs events with `AuditClient`
  - Audit events include type, entity, entity id, and timestamp

---

## Request Flow

### Create Product
1. Client sends `POST /api/products` with product data
2. `ProductController.create()` receives the request
3. Delegates to `ProductService.create()`
4. `ProductService` saves the product with `ProductRepository`
5. Initializes inventory stock via `inventory-service`
6. Logs a `PRODUCT_CREATED` event via `audit-service`
7. Returns the saved product

### Read Product
1. `GET /api/products` or `GET /api/products/active` requested
2. Controller calls service methods to load products
3. Service populates stock from `inventory-service`
4. Returns products with current stock values

### Update Product
1. Client sends `PUT /api/products/{id}` with updated data
2. `ProductService.update()` updates the database record
3. If stock is provided, it updates inventory stock too
4. Logs a `PRODUCT_UPDATED` event

### Delete Product
1. Client sends `DELETE /api/products/{id}`
2. Service fetches product and performs delete
3. Logs a `PRODUCT_DELETED` event

---

## Summary

`product-service` is a product management microservice that not only stores product metadata but also keeps stock synchronized with `inventory-service` and emits audit events. The architecture is:
- controller → service → repository
- with additional external clients for inventory and auditing

This file can be converted to PDF directly.
