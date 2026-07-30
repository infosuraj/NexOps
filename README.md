# NexOps — AI-Driven Operations Platform

**MCA Final Year Project · Amrita School of Computing, Coimbatore**
**Student:** M Suraj Kumar &nbsp;|&nbsp; **Roll No:** AA.SC.P2MCA24070070 &nbsp;|&nbsp; **Batch:** MCA 2024–26

---

## What is NexOps?

NexOps is a multi-agent backend platform built for e-commerce operations. The idea came from a simple problem — managing inventory, pricing, customer support, and supplier orders manually is slow and error-prone. NexOps automates all of this using four autonomous AI agents that communicate through Apache Kafka events.

The platform includes:
- Real-time inventory tracking with automatic low-stock detection
- AI-driven dynamic pricing that raises price when demand is high and lowers it when overstocked
- Smart support ticket triage using Google Gemini AI with load-balanced agent assignment
- Automated purchase orders sent directly to suppliers via email when stock runs low
- A clean admin dashboard built in React with JWT-secured login

This is a fully working system — not a prototype. Every agent is independently deployable and communicates only through events (no direct service-to-service calls, only through the gateway).

---

## Architecture Overview

```
                          ┌─────────────────────────┐
                          │     React Dashboard      │
                          │   (Vite + Axios + JWT)   │
                          └────────────┬────────────┘
                                       │ HTTP (Bearer Token)
                          ┌────────────▼────────────┐
                          │      API Gateway         │
                          │  Spring Cloud Gateway    │
                          │  JWT Auth Filter :8080   │
                          └──┬──────┬──────┬──────┬─┘
                             │      │      │      │
               ┌─────────────▼─┐ ┌──▼──┐ ┌▼────┐ ┌▼────────────────┐
               │  Inventory    │ │Price│ │Escal│ │   Supplier       │
               │  Agent :8081  │ │:8082│ │:8083│ │   Agent :8084    │
               └──────┬────────┘ └──┬──┘ └──┬──┘ └──────┬──────────┘
                      │              │        │            │
                      └──────────────▼────────▼────────────┘
                                  Apache Kafka (Aiven)
                              nexops.stock.update
                              nexops.stock.low
                              nexops.demand.event

                          ┌──────────────────────┐
                          │    PostgreSQL DB       │
                          │  (Railway managed)    │
                          └──────────────────────┘
```

### Event Flow — How the agents work together

1. Admin clicks **Buy −1** on a product → Inventory Agent deducts stock and fires `nexops.stock.update`
2. Pricing Agent consumes that event → if demand is high, it raises the live price and logs the decision
3. If stock hits the low threshold → Inventory Agent also fires `nexops.stock.low`
4. Supplier Agent picks that up → creates a Purchase Order and emails the supplier (via Resend)
5. Support ticket submitted → Escalation Agent calls Gemini AI to triage priority, assigns to the least-loaded human agent, sends a draft reply to the agent and an auto-reply to the customer

---

## Tech Stack

| Layer | Technology |
|---|---|
| Backend | Java 21, Spring Boot 3.2.5, Spring Cloud 2023.0.1 |
| API Gateway | Spring Cloud Gateway (WebFlux / Reactive) |
| Message Broker | Apache Kafka (Aiven cloud, SASL_SSL) |
| Database | PostgreSQL 15 (Railway managed) |
| AI / LLM | Google Gemini 2.5 Flash API |
| Email | Resend Transactional Email API |
| Auth | JWT (JJWT 0.12.3), Spring Security |
| Frontend | React 18, Vite 5, Axios |
| Monitoring | Prometheus + Spring Actuator |
| Containerization | Docker, Docker Compose |
| Deployment | Railway (backend + DB), Aiven (Kafka) |

---

## Project Structure

```
nexops/
├── api-gateway/                        # Spring Cloud Gateway — JWT auth + routing
│   └── src/main/java/com/nexops/gateway/
│       ├── config/RouteConfig.java          # Route definitions for all 4 services
│       ├── security/JwtAuthFilter.java      # Validates Bearer token on every request
│       ├── security/JwtUtil.java            # Token generation + validation (HMAC-SHA256)
│       └── controller/AuthController.java   # POST /api/auth/login
│
├── inventory-service/                  # Agent 1 — Stock tracking & Kafka event publisher
│   └── src/main/java/com/nexops/inventory/
│       ├── entity/Product.java              # Product entity with unique name constraint
│       ├── repository/ProductRepository.java
│       ├── service/InventoryService.java    # CRUD + stock updates + low-stock check
│       ├── kafka/InventoryEventPublisher.java  # Fires stock.update, stock.low, demand events
│       └── controller/InventoryController.java # REST: GET / POST / PUT / DELETE products
│
├── pricing-service/                    # Agent 2 — Dynamic pricing by demand & stock
│   └── src/main/java/com/nexops/pricing/
│       ├── entity/PricingRule.java
│       ├── service/PricingService.java      # Adjusts price on stock & demand events
│       └── kafka/PricingEventConsumer.java  # Listens to demand.event & stock.update
│
├── escalation-service/                 # Agent 3 — AI ticket triage + smart assignment
│   └── src/main/java/com/nexops/escalation/
│       ├── entity/SupportTicket.java        # Ticket with priority, status, AI draft response
│       ├── entity/SupportAgent.java         # Human support agents with load tracking
│       ├── config/DataSeeder.java           # Seeds 5 agents on startup
│       ├── service/EscalationService.java   # Priority detection + load-balanced assignment
│       ├── service/GeminiAIService.java     # Calls Gemini 2.5 Flash for triage + draft reply
│       ├── service/ResendEmailService.java  # Emails agent (with AI draft) + customer
│       └── controller/EscalationController.java
│
├── supplier-service/                   # Agent 4 — Auto purchase orders + supplier email
│   └── src/main/java/com/nexops/supplier/
│       ├── entity/PurchaseOrder.java
│       ├── service/SupplierService.java     # Creates PO with saga idempotency guard
│       ├── service/ResendEmailService.java  # Sends HTML purchase order to supplier
│       └── kafka/SupplierEventConsumer.java # Listens to nexops.stock.low
│
├── frontend/                           # React 18 Admin Dashboard
│   ├── src/
│   │   ├── App.jsx        # All panels: Overview, Inventory, Pricing, Escalation, Supplier
│   │   └── main.jsx
│   ├── public/
│   │   ├── logo.png       # NexOps logo
│   │   └── favicon.ico
│   ├── index.html
│   └── vite.config.js
│
├── Assets/                             # Brand assets (logo, icons)
├── docker-compose.yml                  # Local dev: PostgreSQL + Kafka + ZooKeeper
├── prometheus.yml                      # Metrics scrape config for all services
└── pom.xml                             # Maven parent POM (multi-module build)
```

---

## Features

### 1. Inventory Management
- Add, edit, and delete products through the dashboard
- Each product has a configurable low-stock threshold
- When stock drops to or below threshold → Kafka event is automatically fired
- Duplicate product names are rejected at both the DB level (unique constraint) and service level
- Category is a standardized dropdown — no free-text typos

### 2. AI Dynamic Pricing Agent
- Listens to every stock change and purchase event on Kafka
- Tracks demand per product using a rolling in-memory counter
- Raises price up to 1.5× when demand spikes (5+ purchases in a window)
- Drops price when stock is overstocked and demand is low
- Every decision is logged to the database with the reason and timestamp

### 3. AI Escalation & Support Agent
- Customer submits a ticket — Gemini 2.5 Flash reads the issue and assigns a priority: LOW / MEDIUM / HIGH / CRITICAL
- Smart assignment: picks the least-loaded available agent, with specialization matching
  - FINANCIAL issues → Priya Sharma (billing specialist)
  - TECHNICAL issues → Rahul Verma or Arjun Nair
  - CRITICAL tickets → escalated immediately to the first available agent
  - LOW priority → auto-resolved with AI-drafted reply if agents are at capacity
- Gemini drafts a suggested reply that the agent can review before sending
- Resend API emails the agent with ticket details + AI draft
- Customer gets an automatic acknowledgement with the AI-written response

### 4. Supplier Automation Agent
- Listens for `nexops.stock.low` events on Kafka
- Automatically creates a Purchase Order for 3× the threshold quantity
- Saga idempotency guard: will not create a duplicate PO if one is already PENDING for that product
- Sends a formatted HTML purchase order email to the supplier via Resend API

### 5. JWT Authentication
- Login with `admin / nexops123` to get a signed JWT token (24-hour expiry)
- All API calls from the frontend attach the token as a `Authorization: Bearer` header
- The API Gateway validates the token before forwarding any request to backend services

---

## Running Locally

### Prerequisites

- Java 21 (`brew install openjdk@21` on Mac)
- Maven 3.9+
- Docker Desktop or Colima (for local PostgreSQL + Kafka)
- Node.js 18+

### Step 1 — Start infrastructure

```bash
docker compose up -d
```

This starts PostgreSQL on port `5432` and Kafka on port `9092`.

### Step 2 — Build all services

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@21
export PATH="$JAVA_HOME/bin:$PATH"

mvn clean install -DskipTests
```

### Step 3 — Start each backend service

```bash
# API Gateway
java -jar api-gateway/target/api-gateway-1.0.0.jar

# Inventory Agent
java -jar inventory-service/target/inventory-service-1.0.0.jar \
  --spring.kafka.bootstrap-servers=localhost:9092 \
  --spring.kafka.consumer.properties.security.protocol=PLAINTEXT \
  --spring.kafka.producer.properties.security.protocol=PLAINTEXT

# Pricing Agent
java -jar pricing-service/target/pricing-service-1.0.0.jar \
  --spring.kafka.bootstrap-servers=localhost:9092 \
  --spring.kafka.consumer.properties.security.protocol=PLAINTEXT \
  --spring.kafka.producer.properties.security.protocol=PLAINTEXT

# Escalation Agent
java -jar escalation-service/target/escalation-service-1.0.0.jar \
  --spring.kafka.bootstrap-servers=localhost:9092 \
  --spring.kafka.consumer.properties.security.protocol=PLAINTEXT \
  --spring.kafka.producer.properties.security.protocol=PLAINTEXT

# Supplier Agent
java -jar supplier-service/target/supplier-service-1.0.0.jar \
  --spring.kafka.bootstrap-servers=localhost:9092 \
  --spring.kafka.consumer.properties.security.protocol=PLAINTEXT \
  --spring.kafka.producer.properties.security.protocol=PLAINTEXT
```

### Step 4 — Start the frontend

```bash
cd frontend
npm install
npm run dev
```

Open http://localhost:5173 and log in with `admin / nexops123`.

### Step 5 — Verify all services

```bash
curl http://localhost:8081/actuator/health   # Inventory
curl http://localhost:8082/actuator/health   # Pricing
curl http://localhost:8083/actuator/health   # Escalation
curl http://localhost:8084/actuator/health   # Supplier
```

All should return `{"status":"UP"}`.

---

## Environment Variables

Set these as environment variables when deploying (Railway env vars, `.env` file locally):

| Variable | Description | Used by |
|---|---|---|
| `DATABASE_URL` | PostgreSQL JDBC URL | All services |
| `DB_USER` | Database username | All services |
| `DB_PASSWORD` | Database password | All services |
| `KAFKA_BROKERS` | Kafka bootstrap server address | All services |
| `KAFKA_SASL_JAAS_CONFIG` | Aiven SASL credentials string | All services |
| `KAFKA_SSL_CERT` | Aiven CA certificate (full PEM content) | All services |
| `GEMINI_API_KEY` | Google AI Studio API key | escalation-service |
| `RESEND_API_KEY` | Resend email API key | escalation-service, supplier-service |
| `RESEND_FROM_EMAIL` | Verified sender address | escalation-service, supplier-service |
| `JWT_SECRET` | 256-bit secret for token signing | api-gateway |
| `PORT` | HTTP port for each service | All services |

> Local defaults are defined in each service's `application.yml`. For local Kafka, override `security.protocol` to `PLAINTEXT` as shown above.

---

## API Reference

### Auth
```
POST  /api/auth/login          Body: { "username": "admin", "password": "nexops123" }
                               Returns: { "token": "eyJ..." }
```

### Inventory _(requires Authorization: Bearer token)_
```
GET    /api/inventory/products
POST   /api/inventory/products
PUT    /api/inventory/products/{id}
DELETE /api/inventory/products/{id}
PUT    /api/inventory/products/{id}/stock     Body: { "quantityChange": -1 }
```

### Escalation _(requires Authorization: Bearer token)_
```
GET    /api/escalation/tickets
POST   /api/escalation/tickets    Body: { "customerName", "customerEmail", "issueDescription" }
GET    /api/escalation/tickets/status/{status}
GET    /api/escalation/agents
```

### Supplier _(requires Authorization: Bearer token)_
```
GET    /api/supplier/orders
POST   /api/supplier/orders
PUT    /api/supplier/orders/{id}/status    Body: { "status": "DELIVERED" }
```

### Pricing _(requires Authorization: Bearer token)_
```
GET    /api/pricing/history
GET    /api/pricing/rules
```

---

## Kafka Topics

| Topic | Published by | Consumed by | Purpose |
|---|---|---|---|
| `nexops.stock.update` | inventory-service | pricing-service | Any stock level change |
| `nexops.stock.low` | inventory-service | supplier-service | Triggers auto PO creation |
| `nexops.demand.event` | inventory-service | pricing-service | Purchase event (demand signal) |

---

## Design Patterns Used

| Pattern | Where applied | Why |
|---|---|---|
| Event-Driven Architecture | Kafka between all agents | Loose coupling — agents don't call each other directly |
| Saga (Choreography) | Supplier PO creation | Idempotency guard prevents duplicate purchase orders |
| API Gateway | Spring Cloud Gateway | Single entry point with centralized JWT auth |
| Circuit Breaker + Retry | Pricing → Inventory calls | Resilience4j prevents cascade failure |
| Repository Pattern | All services | Clean separation between data access and business logic |
| Data Seeder | Escalation service startup | Consistent default agents across all environments |

---

## Monitoring

Prometheus metrics are exposed at `/actuator/prometheus` on each service (ports 8081–8084).

The `prometheus.yml` at the project root is pre-configured to scrape all agents. You can connect it to a local Prometheus + Grafana setup or point it to Grafana Cloud.

---

## Deployment (Railway)

The project is deployed on Railway:
- Each microservice is a separate Railway service (5 backend + 1 frontend = 6 total)
- PostgreSQL is provisioned as a Railway plugin — auto-injects `DATABASE_URL`
- Kafka is hosted on Aiven (external) — connected via `KAFKA_BROKERS` env var
- All secrets and API keys are set as Railway environment variables — never committed to code
- Each service has its own `Dockerfile` in the service root directory

---

## Known Limitations

- Dashboard is admin-only — no customer-facing portal (out of scope for this project)
- Gemini AI falls back to demo-mode locally if `GEMINI_API_KEY` is not set
- Supplier emails use placeholder addresses in demo mode; `RESEND_API_KEY` needed for real delivery
- Pricing model uses an in-memory demand counter — a production system would use historical data and an ML model
- No unit tests yet — added to the roadmap

---

## What I Learned

Building NexOps end-to-end taught me how real distributed systems work in practice — not just in theory. A few things that stood out:

**Kafka is not just a queue.** Consumer groups, offset commits, and partition assignment matter a lot once you have multiple consumers and restart scenarios to handle.

**Saga pattern is hard to get right.** The idempotency check in the supplier service felt redundant at first. But when Kafka retried a message and the same low-stock event arrived twice, the guard was the only thing preventing a duplicate purchase order.

**Spring Cloud Gateway + WebFlux** is completely different from Spring MVC. Reactive programming took time to understand — especially the filter chain for JWT validation.

**JWT stateless auth** is elegant but requires careful handling of token expiry on the frontend side. The Axios interceptor pattern made this clean.

**The deployment gap is real.** Code that worked perfectly locally failed on Railway due to environment variable differences, SSL certificate handling for Aiven Kafka, and port binding. Debugging prod issues through Railway logs was a good experience in itself.

---

*Submitted in partial fulfillment of the requirements for the degree of Master of Computer Applications, Amrita School of Computing, Coimbatore.*
