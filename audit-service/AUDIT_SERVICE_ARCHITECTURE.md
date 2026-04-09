# Audit Service Architecture

## Overview

`audit-service` captures and stores audit events from other microservices. It provides a single source of truth for system activity logs.

### Purpose
- Receives audit event data from other services
- Persists audit records in a database
- Provides read access to audit events
- Supports filtering by target type

---

## Technology Stack

- Java + Spring Boot
- Spring Data JPA for persistence
- Spring Web for REST API endpoints
- Jakarta Persistence for entity mapping
- Lombok for dependency injection and constructors

---

## Directory and Component Flow

### `src/main/java/com/dms/audit_service`
This is the main Java package of `audit-service`.

#### `controller/`
- `AuditController.java`
  - Exposes REST API endpoints under `/api/audit`
  - Endpoints:
    - `POST /api/audit/events` — create a new audit event
    - `GET /api/audit/events` — list all audit events or filter by `targetType`
    - `GET /api/audit/events/{id}` — retrieve a specific audit event by id

#### `service/`
- `AuditEventService.java`
  - Business logic for audit operations
  - Saves audit event DTOs to the repository
  - Converts entity objects to DTOs for responses
  - Methods:
    - `create(AuditEventDto dto)` — persist a new audit event
    - `findAll()` — return all audit events
    - `findByTargetType(targetType)` — return events filtered by target type
    - `findById(id)` — return a single event by id

#### `repository/`
- `AuditEventRepository.java`
  - Extends `JpaRepository<AuditEvent, Long>`
  - Adds `findByTargetType(String targetType)` for filtering audit records

#### `entity/`
- `AuditEvent.java`
  - JPA entity mapped to `audit_events` table
  - Fields:
    - `id`
    - `eventType`
    - `actor`
    - `targetType`
    - `targetId`
    - `details`
    - `timestamp`
  - Uses standard getters and setters

#### `dto/`
- `AuditEventDto.java`
  - Data transfer object used by controllers and service layer
  - Contains the same fields as `AuditEvent` plus factory conversion from entity

---

## Request Flow

### Create Audit Event
1. A client service calls `POST /api/audit/events` with event data
2. `AuditController.createEvent()` receives the request
3. It forwards the DTO to `AuditEventService.create()`
4. Service converts DTO to `AuditEvent` entity and saves it with `AuditEventRepository`
5. Returns the saved `AuditEventDto`

### Read Audit Events
1. `GET /api/audit/events` returns all audit events
2. `GET /api/audit/events?targetType=PRODUCT` returns events for a specific target type
3. `GET /api/audit/events/{id}` returns one audit event by id

---

## Integration Points

- Called by other microservices such as:
  - `user-service` for login and registration events
  - `product-service` for product create/update/delete events
  - `order-service` for order lifecycle events

- Acts as the central logging service for important actions and state changes.

---

## Summary

`audit-service` is a lightweight microservice that stores audit events and exposes read APIs. Its architecture is simple and focused on:
- controller → service → repository
- storing and retrieving audit logs
- filtering by audit target type

This file is ready for PDF conversion.
