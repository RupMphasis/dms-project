# DMS Project

This repository contains a set of Spring Boot microservices for the DMS application.

## Services and default ports

| Service | Module | Default port |
|---|---|---|
| Eureka Server | `eureka-server` | `8761` |
| API Gateway | `api-gateway` | `8081` |
| User Service | `user-service` | `8082` |
| Order Service | `order-service` | `8083` |
| Product Service | `product-service` | `8084` |
| Inventory Service | `inventory-service` | `8085` |
| Audit Service | `audit-service` | `8086` |
| Distributor Service | `distributor-service` | `8087` |

> Note: This README uses a consistent local port mapping for the services. If your current `application.properties` files use different ports, update the values there to match your local setup.

## Required dependencies

- Java 17
- Maven (or use the included Maven wrapper `mvnw.cmd` on Windows)
- MySQL running locally on `localhost:3306`
- Database: `dms_db`

## Database setup

Create the `dms_db` database if it does not exist:

```sql
CREATE DATABASE IF NOT EXISTS dms_db;
```

The services are configured with the default MySQL credentials:

- username: `root`
- password: `root@39`

If your credentials are different, update the database settings in each service's `src/main/resources/application.properties` file.

## Start the application as a whole

1. Start the Eureka server first:

```powershell
cd eureka-server
.\mvnw.cmd spring-boot:run
```

2. Start the services one by one in separate terminals:

```powershell
cd audit-service
.\mvnw.cmd spring-boot:run

cd ..\inventory-service
.\mvnw.cmd spring-boot:run

cd ..\product-service
.\mvnw.cmd spring-boot:run

cd ..\order-service
.\mvnw.cmd spring-boot:run

cd ..\user-service
.\mvnw.cmd spring-boot:run

cd ..\api-gateway
.\mvnw.cmd spring-boot:run
```

3. Optionally start the distributor service if needed:

```powershell
cd ..\distributor-service
.\mvnw.cmd spring-boot:run
```

## Start services individually

Each service can be run separately from its own folder:

```powershell
cd <module-name>
.\mvnw.cmd spring-boot:run
```

For example:

```powershell
cd user-service
.\mvnw.cmd spring-boot:run
```

## Access URLs

- Eureka dashboard: `http://localhost:8761`
- API Gateway: `http://localhost:8081`
- User service UI: `http://localhost:8082/users/login`

## Notes

- Services use Eureka for discovery, but they also include direct URLs in configuration to simplify local testing.
- The user-service UI is the main entry point for admin and distributor workflows.
- Audit logs are stored by `audit-service` and can be viewed from the admin UI if that endpoint is enabled.

## Troubleshooting

- If a port is already in use, update the `server.port` value in the relevant service `application.properties` file.
- If the database schema is missing, verify `spring.jpa.hibernate.ddl-auto=update` is enabled in the service configuration.
- If services fail to register with Eureka, make sure Eureka is running on `http://localhost:8761/eureka/`.
