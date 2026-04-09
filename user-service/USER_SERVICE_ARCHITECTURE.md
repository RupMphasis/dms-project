# User Service Architecture

## Overview

`user-service` is the authentication and user management microservice in this architecture.

### Purpose
- Handles user registration and login
- Manages user profiles
- Supports two roles: `ADMIN` and `DISTRIBUTOR`
- Provides both REST API endpoints and server-rendered UI pages
- Integrates with other microservices for distributor creation and audit logging

---

## Technology Stack

- Java + Spring Boot
- Spring Data JPA for persistence
- Spring Security for authentication
- BCrypt for password hashing
- Thymeleaf templates for UI pages
- `RestTemplate` for HTTP communication with other microservices
- Lombok for reducing boilerplate
- Jakarta Validation for request validation

---

## Directory and Component Flow

### `src/main/java/com/dms/user_service`
This is the main Java package for `user-service`.

#### `controller/`
- `UserController.java`
  - REST API endpoints under `/api/users`
  - Handles:
    - `POST /api/users/login`
    - `POST /api/users/register`
    - `PUT /api/users/change-password`
    - `GET /api/users`
    - `GET /api/users/{id}`
    - `DELETE /api/users/{id}`
- `UserUIController.java`
  - UI routes under `/users`
  - Handles web pages for login, registration, dashboards, profile editing, and redirects
  - Uses Thymeleaf templates from `src/main/resources/templates`

#### `service/`
- `UserService.java`
  - Core business logic for user operations
  - Validates login and registration requests
  - Encrypts passwords using `BCryptPasswordEncoder`
  - Saves users through `UserRepository`
  - Calls external services:
    - `distributor-service` for distributor creation
    - `audit-service` via `AuditClient` for event logging
  - Handles profile updates, password changes, and user lookup

#### `repository/`
- `UserRepository.java`
  - Extends `JpaRepository<User, Long>`
  - Provides data access methods such as:
    - `findByUsername(...)`
    - `existsByUsername(...)`
    - `existsByEmail(...)`
    - `findByRole(...)`

#### `entity/`
- `User.java`
  - JPA entity mapped to the `users` table
  - Key fields:
    - `id`
    - `username`
    - `password`
    - `role` (`ADMIN` or `DISTRIBUTOR`)
    - `fullName`
    - `email`
    - `distributorId`
    - `createdAt`
  - Uses `@PrePersist` to automatically set creation time

#### `dto/`
- Data transfer objects used by controllers and service methods
- Examples:
  - `LoginRequest`, `LoginResponse`
  - `RegisterRequest`
  - `ChangePasswordRequest`
  - `UserProfileDto`
  - `DistributorDto`
  - `AuditEventDto`

#### `config/`
- `SecurityConfig.java`
  - Configures Spring Security
  - Defines form login, logout, public paths, and authentication rules
  - Registers `DaoAuthenticationProvider` with `CustomUserDetailsService`
- `RestTemplateConfig.java`
  - Creates the `RestTemplate` bean used for inter-service HTTP calls

#### `security/`
- `CustomUserDetailsService.java`
  - Loads user details from the database for Spring Security authentication
- `CustomUserDetails.java`
  - Wraps the `User` entity into Spring Security `UserDetails`

---

## Templates and UI

### `src/main/resources/templates/`
- Contains Thymeleaf HTML templates used by `UserUIController`
- Example pages:
  - `register.html`
  - `login.html`
  - dashboard pages
  - profile pages

---

## Request Flow

### Registration Flow
1. User visits `/users/register` or calls `/api/users/register`
2. `UserUIController` or `UserController` receives the request
3. Delegates to `UserService.registerUser()`
4. `UserService`:
   - checks for duplicate username/email
   - encodes the password
   - if role is `DISTRIBUTOR`, calls `distributor-service` to create distributor details
   - saves the `User` entity with `UserRepository`
   - logs the event via `AuditClient`
5. Returns success response to the UI or API caller

### Login Flow
1. User submits credentials to `/users/login` or `/api/users/login`
2. `UserService.login()`:
   - fetches user by username
   - verifies password with BCrypt
   - logs the login event
   - builds a `LoginResponse`
3. If UI login, Spring Security creates the session and redirects as needed

### Data Persistence
- `UserRepository` persists `User` entities to the `users` table
- Spring Data JPA and Hibernate manage database interactions

---

## Summary

`user-service` is the dedicated authentication and user management service. It handles user lifecycle operations, protects routes with Spring Security, serves both web UI and API endpoints, and collaborates with `distributor-service` and `audit-service` for distributor onboarding and event logging.

If needed, the same format can be created for `order-service`, `product-service`, and `audit-service` next.
