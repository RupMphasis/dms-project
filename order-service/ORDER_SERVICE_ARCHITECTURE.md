# Order Service Architecture

## Overview

`order-service` is the microservice responsible for order handling and order lifecycle in the system.

### Purpose
- Manages order creation, reading, status updates, and deletion
- Tracks orders for distributors
- Coordinates with inventory and product services
- Sends audit events when orders are created, updated, or deleted

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

---

## Directory and Component Flow

### `src/main/java/com/dms/order_service`
This is the main Java package of `order-service`.

#### `controller/`
- `OrderController.java`
  - REST API endpoints under `/api/orders`
  - Endpoints:
    - `GET /api/orders` — list all orders
    - `GET /api/orders/distributor/{distributorId}` — list orders by distributor
    - `GET /api/orders/{id}` — fetch one order by id
    - `POST /api/orders` — create a new order
    - `PATCH /api/orders/{id}/status` — update order status directly
    - `POST /api/orders/{id}/decision` — update order status with admin message
    - `DELETE /api/orders/{id}` — delete an order

#### `service/`
- `OrderService.java`
  - Core business logic for order lifecycle
  - Uses `OrderRepository` for database persistence
  - Uses external clients to interact with other services:
    - `InventoryClient` to adjust inventory stock
    - `ProductClient` to fetch product data when inventory is missing
    - `AuditClient` to log order events
  - Key methods:
    - `findAll()` — returns all orders
    - `findByDistributor(distributorId)` — returns distributor-specific orders
    - `getById(id)` — retrieves one order
    - `create(order)` — creates a new order and optionally adjusts inventory when status is `APPROVED`
    - `updateStatus(id, status, adminMessage)` — changes order status, adjusts stock when approving or canceling, and saves admin notes
    - `delete(id)` — deletes the order and restores inventory if necessary

#### `repository/`
- `OrderRepository.java`
  - Extends `JpaRepository<OrderEntity, Long>`
  - Adds custom lookups:
    - `findByDistributorId(Long distributorId)`
    - `findByProductId(Long productId)`

#### `entity/`
- `OrderEntity.java`
  - JPA entity mapped to `orders` table
  - Fields include:
    - `id`
    - `productId`
    - `distributorId`
    - `quantity`
    - `customerName`
    - `customerPhone`
    - `shippingAddress`
    - `shippingCity`
    - `shippingPostalCode`
    - `status`
    - `orderDetails` relationship
    - `createdAt`
  - Contains helper transient fields for `customMessage`, `fulfillmentTime`, and `adminMessage`
  - Uses `@PrePersist` to set creation time and default status to `PENDING`

- `OrderDetails.java`
  - JPA entity mapped to `order_details` table
  - Stores optional details such as:
    - `customMessage`
    - `fulfillmentTime`
    - `adminMessage`

#### `dto/`
- `OrderDecisionDto.java`
  - Accepts status and optional admin message for decision endpoints

---

## Service Interactions

- `inventory-service`
  - `order-service` adjusts stock when orders are approved or deleted
  - If inventory data is missing, it creates inventory records from product data

- `product-service`
  - Used to fetch product metadata and current stock when inventory item is not found

- `audit-service`
  - Logs order lifecycle actions such as creation, status updates, and deletion

---

## Request Flow

### Create Order
1. Client sends `POST /api/orders` with an order payload
2. `OrderController.create()` receives the request
3. `OrderService.create()`:
   - checks order status
   - if status is `APPROVED`, attempts inventory stock adjustment
   - if inventory item is missing, fetches product data and creates inventory
   - saves the order in the database
   - logs `ORDER_CREATED` to audit service
4. Returns the saved order

### Update Order Status
1. Client sends `PATCH /api/orders/{id}/status` or `POST /api/orders/{id}/decision`
2. `OrderController` forwards the request to `OrderService.updateStatus()`
3. Service logic handles:
   - approving orders (decrement inventory)
   - canceling approved orders (restore inventory)
   - adding admin messages to orders
   - saving the updated order
   - logging `ORDER_STATUS_UPDATED` to audit service

### Delete Order
1. Client sends `DELETE /api/orders/{id}`
2. `OrderService.delete()`:
   - restores inventory if order was not already canceled
   - logs `ORDER_DELETED`
   - deletes the order record

---

## Summary

`order-service` manages the order lifecycle and integrates tightly with inventory, product, and audit services. Its architecture is:
- controller → service → repository
- plus external service clients for inventory synchronization and audit logging

This file is ready for PDF conversion.
