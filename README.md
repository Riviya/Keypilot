# 🚀 KeyPilot

**KeyPilot** is a backend system for **secure API key management, intelligent key rotation, rate limiting, and proxy-based request forwarding** — built using **Clean Architecture** and **strict Test-Driven Development (TDD)**.

---

## 🧠 What Problem Does KeyPilot Solve?

When working with external APIs (e.g., OpenAI), managing multiple API keys becomes challenging:

* Keys hit rate limits
* Keys need rotation
* Keys must not be exposed
* Requests need controlled routing

**KeyPilot solves this by acting as a secure gateway that:**

* Stores and manages API keys safely
* Rotates keys intelligently
* Applies per-key rate limits
* Proxy requests without exposing credentials

---

## 🏗️ Architecture

KeyPilot follows **Clean Architecture (Ports & Adapters)**:

```
presentation  → Controllers (HTTP layer)
application   → Use cases / orchestration
domain        → Core business logic
infrastructure→ External systems (file storage, HTTP clients)
```

### Key Principles:

* Domain is framework-independent
* Dependencies point inward
* Infrastructure is replaceable
* High testability across layers

---

## 🔑 Core Features

### 1. API Key Management

* Add, list, delete keys
* Strong domain encapsulation
* Validation via DTOs

---

### 2. Key Rotation Strategies

* **Round Robin** (thread-safe)
* **Random Selection**
* Easily extendable via a strategy pattern

---

### 3. Rate Limiting

* Sliding window per API key
* Thread-safe implementation
* Keys dynamically filtered before selection

---

### 4. Secure Proxy Layer

* Forwards requests to external providers
* Injects managed API keys automatically
* Removes incoming Authorization headers
* Handles provider failures gracefully

---

### 5. Persistence

* File-based JSON storage
* Thread-safe (ReadWriteLock)
* Restart-safe key persistence

---

### 6. CLI Tool (Go)

* Manage keys via command line
* Secure (no API key exposure)
* Supports configurable gateway endpoint

---

## 🔐 Security

* API keys are never logged or returned
* Incoming credentials are stripped before forwarding
* Controlled outbound communication via gateway abstraction

---

## 🧪 Testing Philosophy

KeyPilot is built with **strict TDD**:

* Tests written before implementation
* Red → Green → Refactor cycle followed consistently
* Concurrency scenarios explicitly validated
* No real network calls in tests

---

## ⚙️ Design Highlights

* Strategy pattern for extensibility
* Domain-driven design principles
* Thread safety using atomic primitives
* Clear separation of concerns

---

## 📌 Example Flow

1. Client sends request to KeyPilot
2. KeyPilot:

    * Selects an available API key
    * Applies rate limiting
    * Injects the key into the request
3. Request is forwarded to external provider
4. Response is returned to a client

---

## 🚧 Roadmap

* Distributed rate limiting (Redis)
* Observability (metrics, logging, tracing)
* Authentication & access control
* Multi-provider support
* Containerization & deployment pipeline

---

## 💡 Why This Project Matters

KeyPilot demonstrates:

* Clean Architecture in practice
* Real-world system design
* Concurrency handling in Java
* Strict TDD discipline
* Secure API gateway design patterns

---

## 🏁 Status

**v1.0.0 — Initial stable release**

---

## 👨‍💻 Author

Built as a system design and backend engineering project focused on production-grade practices.

---
