# User Management System

Enterprise-grade User Management System with Role-Based Access Control (RBAC), JWT authentication, event-driven architecture (Apache Kafka), built with Spring Boot 3.x and MySQL.

---

## 🏗️ Architecture

```
┌──────────────────────────────────────────────────────┐
│                    Client / Swagger UI               │
└──────────────────────┬───────────────────────────────┘
                       │ REST API (JSON)
┌──────────────────────▼───────────────────────────────┐
│              Controller Layer                         │
│  UserController │ RoleController │ AdminController    │
└──────────────────────┬───────────────────────────────┘
                       │
┌──────────────────────▼───────────────────────────────┐
│               Service Layer                           │
│    UserService │ RoleService │ AuditLogService        │
└────────┬─────────────┬───────────────────────────────┘
         │             │
    ┌────▼────┐   ┌────▼────────────────────────┐
    │ JPA     │   │ Kafka Event Publisher        │
    │ Repos   │   │ (Registration / Login events)│
    └────┬────┘   └────┬────────────────────────┘
         │             │
    ┌────▼────┐   ┌────▼────┐
    │ MySQL   │   │ Kafka   │
    └─────────┘   └─────────┘
```

### Layered Pattern

| Layer | Responsibility |
|-------|---------------|
| **Controller** | HTTP endpoints, request validation, response formatting |
| **Service** | Business logic, authentication, event publishing |
| **Repository** | Data access via Spring Data JPA |
| **Security** | JWT generation/validation, authentication filter, RBAC |
| **Event** | Async event publishing to Apache Kafka |

---

## 🚀 Tech Stack

| Technology | Version | Purpose |
|-----------|---------|---------|
| Spring Boot | 3.3.6 | Application framework |
| Spring Security | 6.x | Authentication & authorization |
| JWT (jjwt) | 0.12.6 | Stateless token-based auth |
| MySQL | 8.0 | Relational database |
| Apache Kafka | 3.5.0+ | Distributed event streaming |
| Zookeeper | 3.8.x | Distributed coordination for Kafka |
| Hibernate | 6.x | ORM / JPA implementation |
| Lombok | Latest | Boilerplate reduction |
| springdoc-openapi | 2.6.0 | Swagger / OpenAPI docs |
| Docker | Latest | Containerization |
| H2 | Latest | In-memory DB for tests |

---

## 📡 API Endpoints

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| `POST` | `/api/users/register` | Public | Register new user |
| `POST` | `/api/users/login` | Public | Login & get JWT |
| `GET` | `/api/users/me` | JWT | Get current user profile |
| `POST` | `/api/users/{userId}/roles` | ADMIN | Assign role to user |
| `POST` | `/api/roles` | ADMIN | Create a new role |
| `GET` | `/api/admin/stats` | ADMIN | System statistics |

---

## 🛠️ Setup & Running

### Prerequisites

- **Java 17+**
- **Maven 3.9+** (or use included Maven Wrapper)
- **Docker & Docker Compose** (for containerized setup)

### Option 1: Local Development

1. **Start Infrastructure** (MySQL, Zookeeper, Kafka):
   ```bash
   docker-compose up mysql zookeeper kafka -d
   ```

2. **Run the application**:
   ```bash
   ./mvnw spring-boot:run
   ```
   On Windows:
   ```bash
   mvnw.cmd spring-boot:run
   ```

3. **Access Swagger UI**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

### Option 2: Full Docker Setup

```bash
docker-compose up --build
```

This starts all services:
- **App**: `http://localhost:8080`
- **MySQL**: `localhost:3306`
- **Kafka**: `localhost:9092`
- **Zookeeper**: `localhost:2181`

### Running Tests

```bash
./mvnw test
```

Tests use H2 in-memory database — no external dependencies required.

---

## 🔐 Authentication Flow

```
1. POST /api/users/register (or /login)
   ↓
2. Server validates credentials, generates JWT
   ↓
3. Client stores JWT
   ↓
4. Client sends: Authorization: Bearer <token>
   ↓
5. JwtAuthenticationFilter extracts & validates token
   ↓
6. SecurityContext set → request proceeds to controller
   ↓
7. @PreAuthorize checks role-based access
```

### JWT Token Contents

```json
{
  "sub": "user@example.com",
  "roles": "ROLE_USER,ROLE_ADMIN",
  "iat": 1708300000,
  "exp": 1708386400
}
```

---

## 📨 Event-Driven Architecture

Events are published to **Apache Kafka** asynchronously on:

| Event | Topic | Partition Key |
|-------|-------|---------------|
| User Registration | `user.registration` | `email` |
| User Login | `user.login` | `email` |

### Event Payload

```json
{
  "eventType": "USER_REGISTERED",
  "userId": 1,
  "email": "user@example.com",
  "timestamp": "2026-02-19T17:00:00"
}
```

---

## 📂 Project Structure

```
src/main/java/com/usermanagement/
├── config/          # Security, Cache, Kafka, OpenAPI configs
├── controller/      # REST controllers
├── dto/
│   ├── request/     # Request DTOs with validation
│   └── response/    # Response DTOs
├── entity/          # JPA entities (User, Role)
├── event/           # Kafka event model & publisher
├── exception/       # Custom exceptions & global handler
├── mapper/          # DTO mapping utilities
├── repository/      # Spring Data JPA repositories
├── security/        # JWT provider, filter, UserDetailsService
└── service/         # Business logic layer
```

---

## 📋 Design Decisions

| Decision | Rationale |
|----------|-----------|
| **Stateless JWT** | No server-side session storage → horizontally scalable |
| **EAGER fetch for roles** | Roles are always needed for security context — avoids LazyInit issues |
| **BCrypt password hashing** | Industry standard, adaptive hashing |
| **Apache Kafka** | High-throughput, durable, and replayable distributed log |
| **Partitioning by Email** | Ensures all events for the same user are processed in order |
| **@Async event publishing** | Non-blocking — registration/login latency unaffected |
| **Manual DTO mapping** | No annotation processor dependency; explicit control |
| **H2 for tests** | Fast, zero-config test database |
| **Multi-stage Docker build** | Smaller production implementation footprint |
| **Excluding Kafka in Tests** | Tests use mocks for `KafkaTemplate` to remain fast and standalone |

---

## ⚠️ Assumptions

1. Email is the unique identifier used for authentication (not username)
2. Default `ROLE_USER` is auto-assigned on registration if it exists in the DB
3. JWT secret is configured in `application.yml`
4. Kafka connection failures are logged but do not block registration/login
5. `ddl-auto: update` is used for Convenience — use formal migrations for production
6. Caching uses in-memory `ConcurrentMapCacheManager`
