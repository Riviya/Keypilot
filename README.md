<div align="center">

```
  ██╗  ██╗███████╗██╗   ██╗██████╗ ██╗██╗      ██████╗ ████████╗
  ██║ ██╔╝██╔════╝╚██╗ ██╔╝██╔══██╗██║██║     ██╔═══██╗╚══██╔══╝
  █████╔╝ █████╗   ╚████╔╝ ██████╔╝██║██║     ██║   ██║   ██║
  ██╔═██╗ ██╔══╝    ╚██╔╝  ██╔═══╝ ██║██║     ██║   ██║   ██║
  ██║  ██╗███████╗   ██║   ██║     ██║███████╗╚██████╔╝   ██║
  ╚═╝  ╚═╝╚══════╝   ╚═╝   ╚═╝     ╚═╝╚══════╝ ╚═════╝    ╚═╝
```

**The local AI API gateway for developers who are tired of managing API keys.**

Key rotation · Rate limiting · Multi-provider routing · One command to install

<br/>

[![License: MIT](https://img.shields.io/badge/License-MIT-green.svg?style=for-the-badge)](LICENSE)
[![Docker](https://img.shields.io/badge/Docker-riviyaaa%2Fkeypilot-2496ED?style=for-the-badge&logo=docker&logoColor=white)](https://hub.docker.com/r/riviyaaa/keypilot)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Go](https://img.shields.io/badge/Go-1.22-00ADD8?style=for-the-badge&logo=go&logoColor=white)](https://golang.org)
[![CI](https://img.shields.io/github/actions/workflow/status/riviya/keypilot/ci.yml?style=for-the-badge&label=CI)](https://github.com/riviya/keypilot/actions)
[![Contributions Welcome](https://img.shields.io/badge/contributions-welcome-orange?style=for-the-badge)](https://github.com/riviya/keypilot/issues)

<br/>

[Getting Started](#-getting-started) · [How It Works](#-how-it-works) · [CLI Reference](#-cli-reference) · [Configuration](#-configuration) · [Contributing](#-contributing) · [Roadmap](#-roadmap)

</div>

---

## The Problem

Every developer building with AI APIs runs into the same wall:

- You have **multiple API keys** across projects with no clean rotation strategy
- One key hits a **rate limit** and your entire application errors out with a `429`
- Switching between **OpenAI, Anthropic, and Gemini** means different base URLs, different auth patterns, different SDK configs
- Your keys are sitting in **plaintext** `.env` files scattered across your machine

KeyPilot fixes all of this. It runs locally as a single gateway that your apps talk to instead of calling AI providers directly. Key rotation, rate limiting, and provider routing happen automatically — your code never changes.

---

## How It Works

```
Your Application
       │
       │  POST http://localhost:4000/v1/chat/completions
       │  (no API key needed in your app)
       ▼
┌──────────────────────────────────────────────────────┐
│                  KeyPilot Gateway                    │
│                  (localhost:4000)                    │
│                                                      │
│  ┌──────────────────┐   ┌────────────────────────┐  │
│  │   Key Rotation   │   │     Rate Limiter        │  │
│  │  · Round-robin   │   │  · Per-key quotas       │  │
│  │  · Random        │   │  · Auto cooldown        │  │
│  └──────────────────┘   └────────────────────────┘  │
│                                                      │
│  ┌──────────────────┐   ┌────────────────────────┐  │
│  │  Auto Retry      │   │  Provider Routing       │  │
│  │  · 429 handling  │   │  · OpenAI               │  │
│  │  · Key switching │   │  · Anthropic            │  │
│  └──────────────────┘   │  · Gemini + more        │  │
│                          └────────────────────────┘  │
└──────────────────────────────────────────────────────┘
       │
       ├──→  api.openai.com                    Bearer token
       ├──→  api.anthropic.com                 Bearer token
       └──→  generativelanguage.googleapis.com  ?key=
```

---

## Features

| Feature | Description |
|---|---|
| **Key Rotation** | Round-robin and random strategies across multiple keys per provider |
| **Rate Limiting** | Per-key sliding window with automatic cooldown and recovery |
| **Auto Retry on 429** | Switches to the next available key transparently — your app never sees a 429 |
| **Multi-Provider** | OpenAI, Anthropic, Gemini — route per request via a single header |
| **Per-Provider Auth** | Bearer token, query parameter (`?key=`), or custom header — configured, not coded |
| **Correlation IDs** | Every request tagged with a trace ID for end-to-end log traceability |
| **Live Status API** | `/api/status` shows key health per provider in real time |
| **CLI Management** | Add, list, and delete keys from the terminal |
| **Zero Code Changes** | Change one base URL in your app — nothing else touches your code |
| **Docker Native** | Runs as a container — no Java, no runtime setup required on the developer machine |

---

## Getting Started

### Requirements

- **Docker Desktop** — [Install here](https://docs.docker.com/get-docker/)
- `curl` and `bash` in your terminal
- **Linux** or **Windows** (Git Bash / WSL)

### One-Command Install

```bash
curl -fsSL https://raw.githubusercontent.com/riviya/keypilot/main/install.sh | bash
```

The installer will:

1. Detect your OS and CPU architecture
2. Verify Docker is installed and the daemon is running
3. Fetch the latest release version from GitHub
4. Pull the KeyPilot Docker image from DockerHub
5. Start the KeyPilot container on port `4000`
6. Download and install the `keypilot` CLI binary to `/usr/local/bin`

### Add Your First Keys

```bash
# OpenAI
keypilot keys add --provider openai --key sk-your-openai-key

# Anthropic
keypilot keys add --provider anthropic --key sk-ant-your-key

# Gemini
keypilot keys add --provider gemini --key AIza-your-gemini-key
```

### Verify Everything Is Running

```bash
keypilot status
```

```
✅ Gateway: http://localhost:4000

PROVIDER      STATUS      TOTAL   AVAILABLE  LIMITED   INACTIVE
-----------------------------------------------------------------
openai        available   2       2          0         0
anthropic     available   1       1          0         0
gemini        available   1       1          0         0
```

---

## Pointing Your App at KeyPilot

Change one line. Nothing else.

**Python — OpenAI SDK**
```python
from openai import OpenAI

client = OpenAI(
    base_url="http://localhost:4000/v1",
    api_key="keypilot"          # KeyPilot manages the real keys — this is a placeholder
)

response = client.chat.completions.create(
    model="gpt-4o",
    messages=[{"role": "user", "content": "Hello!"}]
)
```

**Node.js — OpenAI SDK**
```javascript
import OpenAI from 'openai';

const client = new OpenAI({
  baseURL: 'http://localhost:4000/v1',
  apiKey: 'keypilot',
});
```

**Environment variable — works with any SDK**
```bash
export OPENAI_BASE_URL=http://localhost:4000/v1
export OPENAI_API_KEY=keypilot
```

**curl**
```bash
curl -X POST http://localhost:4000/v1/chat/completions \
  -H "Content-Type: application/json" \
  -d '{
    "model": "gpt-4o",
    "messages": [{"role": "user", "content": "Hello!"}]
  }'
```

### Switching Providers per Request

Use the `X-Gateway-Provider` header to route to a specific provider. Your request path stays exactly the same in your application code:

```bash
# Anthropic
curl -X POST http://localhost:4000/v1/messages \
  -H "X-Gateway-Provider: anthropic" \
  -H "Content-Type: application/json" \
  -d '{
    "model": "claude-3-5-sonnet-20241022",
    "max_tokens": 1024,
    "messages": [{"role": "user", "content": "Hello!"}]
  }'

# Gemini
curl -X POST http://localhost:4000/v1beta/models/gemini-2.5-flash:generateContent \
  -H "X-Gateway-Provider: gemini" \
  -H "Content-Type: application/json" \
  -d '{"contents":[{"parts":[{"text":"Hello!"}]}]}'
```

If no header is set, KeyPilot uses the configured default provider (OpenAI by default).

---

## CLI Reference

### `keypilot keys add`
Add an API key for a provider. Multiple keys per provider are fully supported — rotation starts automatically.

```bash
keypilot keys add --provider <name> --key <value>

# Examples
keypilot keys add --provider openai    --key sk-abc123
keypilot keys add --provider openai    --key sk-def456   # second key — rotation begins
keypilot keys add --provider anthropic --key sk-ant-xyz
keypilot keys add --provider gemini    --key AIza-abc
```

### `keypilot keys list`
List all stored keys. Values are **never shown**.

```bash
keypilot keys list
```
```
ID                                      PROVIDER      ACTIVE
--------------------------------------------------------------
3f2a1c8d-...                            openai        true
7b9e4f1a-...                            openai        true
c2d8e3f0-...                            anthropic     true
```

### `keypilot keys delete`
Remove a key by its ID.

```bash
keypilot keys delete --id 3f2a1c8d-...
```

### `keypilot status`
Show live gateway health and per-provider key statistics.

```bash
keypilot status

# Custom gateway URL
keypilot status --gateway http://localhost:4001
```

---

## Configuration

### Config Directory

KeyPilot creates and manages a directory at `~/.keypilot-cli/` on your machine:

```
~/.keypilot-cli/
└── keys.json       ← stored API keys (managed automatically)
```

This directory is mounted into the container as a volume, so keys persist across container restarts and image updates.

### Provider Configuration

Providers are configured via environment variables passed to the container. The `docker-compose.yml` in the project root is the canonical place to configure them:

```yaml
environment:
  # ── OpenAI ────────────────────────────────────────────
  GATEWAY_PROVIDERS_OPENAI_BASE_URL: https://api.openai.com
  GATEWAY_PROVIDERS_OPENAI_AUTH_TYPE: BEARER

  # ── Anthropic ─────────────────────────────────────────
  GATEWAY_PROVIDERS_ANTHROPIC_BASE_URL: https://api.anthropic.com
  GATEWAY_PROVIDERS_ANTHROPIC_AUTH_TYPE: BEARER

  # ── Gemini ────────────────────────────────────────────
  GATEWAY_PROVIDERS_GEMINI_BASE_URL: https://generativelanguage.googleapis.com
  GATEWAY_PROVIDERS_GEMINI_AUTH_TYPE: QUERY_PARAM
  GATEWAY_PROVIDERS_GEMINI_AUTH_PARAM_NAME: key

  # ── Defaults ──────────────────────────────────────────
  GATEWAY_DEFAULT_PROVIDER: openai
  GATEWAY_RETRY_MAX_ATTEMPTS: "3"
```

After editing, restart the container:
```bash
docker compose down && docker compose up -d
```

### Adding a Custom Provider

Any HTTP-based AI API works — no code changes required:

```yaml
# Bearer token auth (most providers)
GATEWAY_PROVIDERS_MYPROVIDER_BASE_URL: https://api.myprovider.com
GATEWAY_PROVIDERS_MYPROVIDER_AUTH_TYPE: BEARER

# Query parameter auth (Gemini-style)
GATEWAY_PROVIDERS_MYPROVIDER_BASE_URL: https://api.myprovider.com
GATEWAY_PROVIDERS_MYPROVIDER_AUTH_TYPE: QUERY_PARAM
GATEWAY_PROVIDERS_MYPROVIDER_AUTH_PARAM_NAME: api_key

# Custom header auth
GATEWAY_PROVIDERS_MYPROVIDER_BASE_URL: https://api.myprovider.com
GATEWAY_PROVIDERS_MYPROVIDER_AUTH_TYPE: API_KEY_HEADER
GATEWAY_PROVIDERS_MYPROVIDER_AUTH_PARAM_NAME: x-api-key
```

---

## Status & Health API

KeyPilot exposes a REST management API directly on the gateway.

**Health check**
```bash
curl http://localhost:4000/health
# {"status":"UP","service":"api-gateway"}
```

**Live status**
```bash
curl http://localhost:4000/api/status
```
```json
{
  "healthy": true,
  "providers": [
    {
      "providerName": "openai",
      "available": true,
      "totalKeys": 2,
      "availableKeys": 2,
      "rateLimitedKeys": 0,
      "inactiveKeys": 0
    }
  ]
}
```

**Key management (what the CLI calls)**
```bash
# Add
curl -X POST http://localhost:4000/api/keys \
  -H "Content-Type: application/json" \
  -d '{"provider":"openai","keyValue":"sk-..."}'

# List
curl http://localhost:4000/api/keys

# Delete
curl -X DELETE http://localhost:4000/api/keys/{id}
```

---

## Architecture

KeyPilot is built with Clean Architecture — strict layer separation enforced throughout.

```
┌────────────────────────────────────────────────────────────────┐
│                       Presentation Layer                       │
│          REST Controllers  ·  GlobalExceptionHandler           │
├────────────────────────────────────────────────────────────────┤
│                       Application Layer                        │
│    ApiKeyService  ·  ProxyService  ·  RetryHandler             │
│    KeyRotationService  ·  RateLimiterService                   │
│    GatewayStatusService  ·  ProviderNotFoundException          │
├────────────────────────────────────────────────────────────────┤
│                         Domain Layer                           │
│    ApiKey  ·  KeyRotationStrategy  ·  ProviderGateway          │
│    Provider  ·  ProviderRegistry  ·  ProxyRequest              │
├────────────────────────────────────────────────────────────────┤
│                     Infrastructure Layer                       │
│    FileBackedApiKeyRepository  ·  HttpProviderGateway          │
│    InMemoryProviderRegistry  ·  ProviderAuthStrategyFactory    │
│    RequestLoggingFilter                                        │
└────────────────────────────────────────────────────────────────┘
```

### Tech Stack

| Component | Technology | Purpose |
|---|---|---|
| Gateway server | Java 21, Spring Boot 3.2 | Proxy engine, key management REST API |
| CLI | Go 1.22, Cobra | Key management terminal interface |
| Persistence | JSON file (`~/.keypilot-cli/keys.json`) | API key storage |
| Container | Docker (`eclipse-temurin:21-jre-alpine`) | Portable runtime — no Java required on host |
| CI | GitHub Actions | Test on every push, release on every version tag |
| Registry | DockerHub (`riviyaaa/keypilot`) | Public image distribution |

### Repository Structure

```
riviya/keypilot/
├── src/                            ← Spring Boot gateway source
│   └── main/java/.../gateway/
│       ├── domain/                 ← Business rules (zero framework dependencies)
│       │   ├── model/              ·  ApiKey entity
│       │   ├── rotation/           ·  Key rotation strategies (Strategy Pattern)
│       │   ├── proxy/              ·  ProxyRequest, ProviderGateway interface
│       │   └── provider/           ·  Provider, ProviderRegistry interface
│       ├── application/            ← Orchestration + service logic
│       │   ├── service/            ·  ApiKeyService, ProxyService, RetryHandler
│       │   ├── port/               ·  Repository interface (ApiKeyRepository)
│       │   ├── dto/                ·  Request/response DTOs
│       │   └── retry/              ·  RetryPolicy, RetryExhaustedException
│       ├── infrastructure/         ← External concerns
│       │   ├── persistence/        ·  FileBackedApiKeyRepository
│       │   ├── proxy/              ·  HttpProviderGateway, auth strategies
│       │   ├── filter/             ·  RequestLoggingFilter (correlation IDs)
│       │   └── config/             ·  Spring beans, ProviderProperties
│       └── presentation/           ← HTTP layer
│           ├── controller/         ·  ProxyController, ApiKeyController, StatusController
│           └── exception/          ·  GlobalExceptionHandler
├── cli/                            ← Go CLI source
│   ├── cmd/                        ·  Cobra commands (keys add/list/delete, status)
│   ├── client/                     ·  HTTP client for gateway REST API
│   └── model/                      ·  Shared data types
├── .github/workflows/
│   ├── ci.yml                      ← Tests on every push and PR
│   └── release.yml                 ← DockerHub push + GitHub Release on version tag
├── Dockerfile                      ← Multi-stage build (JDK builder + JRE runtime)
├── docker-compose.yml              ← Local development and provider configuration
├── install.sh                      ← One-command installer for Linux/Windows
└── pom.xml                         ← Maven build configuration
```

---

## Contributing

KeyPilot is open source and actively welcomes contributions — bug fixes, new provider integrations, documentation improvements, and well-scoped features.

### Development Setup

**Prerequisites:**
- Java 21+
- Maven (bundled via `./mvnw` — no separate install needed)
- Go 1.22+
- Docker Desktop

**Clone and run tests:**

```bash
git clone https://github.com/riviya/keypilot.git
cd keypilot

# Run gateway unit tests
./mvnw test

# Run gateway integration tests (requires Docker)
./mvnw verify

# Build and test the CLI
cd cli
go build -o keypilot .
go test ./... -v -race
```

**Run without Docker during development:**

```bash
# Terminal 1 — start gateway directly (hot reload available)
./mvnw spring-boot:run

# Terminal 2 — use CLI against it
cd cli
go run . keys add --provider openai --key sk-test-123
go run . status
```

### Contribution Guidelines

**The non-negotiables:**

- **TDD is required.** Write the failing test first, then the implementation. PRs without tests for new behaviour will be asked to revise before review.
- **Respect layer boundaries.** Domain code has zero Spring annotations. Infrastructure code does not import application services. The compiler does not enforce this — your reviewer will.
- **Never log key values.** Not in debug, not in tests, not in error messages. Key IDs are fine. Raw key values are not.
- **Name things clearly.** No abbreviations in production code. Code is read far more than it is written.

**Submitting a PR:**

1. Fork the repository and create a branch from `main`:
   ```bash
   git checkout -b feat/your-feature-name
   ```

2. Write your test first, then the implementation

3. Confirm all tests pass:
   ```bash
   ./mvnw verify
   cd cli && go test ./... -race
   ```

4. Use a clear commit message:
   ```
   feat(rotation): add weighted key rotation strategy
   fix(retry):     handle connection timeout as non-retryable
   docs(readme):   add LangChain integration example
   test(proxy):    add Authorization header stripping integration test
   ```

5. Open a PR against `main` with a description of what problem it solves

### Good First Issues

Well-scoped starting points if you are new to the project:

- Add `keypilot keys disable --id <uuid>` — soft-disable without deletion
- Add `--output json` flag to `keypilot status` for scripting and CI use
- Add provider configuration validation on gateway startup with descriptive error messages
- Improve the Linux error message when Docker is running but the user is not in the `docker` group
- Write a `CONTRIBUTING.md` with expanded setup notes for Windows contributors

---

## Roadmap

| Status | Item |
|---|---|
| ✅ Complete | Key rotation — round-robin + random strategies |
| ✅ Complete | Rate limiting with per-key sliding window |
| ✅ Complete | Automatic retry on 429 with transparent key switching |
| ✅ Complete | Multi-provider routing (OpenAI, Anthropic, Gemini) |
| ✅ Complete | Per-provider auth strategies (Bearer, QueryParam, custom header) |
| ✅ Complete | Correlation ID tracing on every request |
| ✅ Complete | Live status API (`/api/status`) |
| ✅ Complete | Multi-stage Docker build + DockerHub (`riviyaaa/keypilot`) |
| ✅ Complete | GitHub Actions CI/CD pipeline |
| ✅ Complete | One-command install script — Linux and Windows |
| ✅ Complete | MIT License |
| 🔄 In Progress | AES-256-GCM encryption of stored keys at rest |
| 🔄 In Progress | Self-updating CLI (`keypilot update`) |
| 📋 Planned | macOS native install + Homebrew tap |
| 📋 Planned | Weighted key rotation strategy |
| 📋 Planned | Usage analytics — `keypilot analytics` |
| 📋 Planned | Key expiry and rotation alerts |
| 📋 Planned | Team mode — shared key pool across multiple developers |
| 💡 Considering | Slack/webhook alert on rate limit exhaustion |
| 💡 Considering | Export usage report as CSV |

Have an idea not on this list? [Open a feature request](https://github.com/riviya/keypilot/issues/new).

---

## Security

**Responsible disclosure:** If you discover a security vulnerability, please do **not** open a public GitHub issue. Use [GitHub's private security advisory](https://github.com/riviya/keypilot/security/advisories/new) feature instead. We will respond within 48 hours.

**Current security posture:**

- API key values are **never returned** in any REST API response
- Key values are **never written to logs** — only key IDs appear in log output
- The gateway container runs as a **non-root user** (`gateway:gateway`)
- All gateway traffic is local — `localhost` only by default
- No key data is baked into the Docker image — keys live in the mounted host volume only

> ⚠️ **Note on encryption at rest:** `~/.keypilot-cli/keys.json` currently stores key values in plaintext JSON. AES-256-GCM encryption of stored keys is actively in development and is the next major release item. Until then, ensure the file has restricted permissions (`chmod 600 ~/.keypilot-cli/keys.json`) and that your machine uses full-disk encryption.

---

## FAQ

**Does KeyPilot send my API keys to any external service?**
No. KeyPilot runs entirely on your local machine. Your keys go directly from your machine to the AI provider. There is no KeyPilot cloud service, telemetry, or analytics.

**What happens if the container crashes?**
The Docker container is configured with `restart: unless-stopped`. It restarts automatically. Keys are persisted to `~/.keypilot-cli/keys.json` and survive both restarts and image updates.

**Can I use KeyPilot with LangChain, LlamaIndex, or similar frameworks?**
Yes. Any library that allows a custom base URL works with KeyPilot. Set `base_url=http://localhost:4000/v1` and use your provider's normal path structure.

**Can I run KeyPilot on a port other than 4000?**
Yes. Edit `docker-compose.yml` and change the port mapping, then restart the container.

**Can I add multiple keys for the same provider?**
Yes — this is KeyPilot's core use case. Add as many keys as you have and rotation begins immediately across all of them.

**Can I use KeyPilot in a CI/CD pipeline?**
KeyPilot is designed as a local developer tool. For CI/CD, inject API keys directly via pipeline environment variables rather than running a local gateway.

---

## License

KeyPilot is released under the [MIT License](LICENSE).

Copyright (c) 2025 [riviya](https://github.com/riviya)

---

<div align="center">

Built with Java, Go, and a lot of frustration with API rate limits.

**Star ⭐ the repo if KeyPilot saves you time.**

[GitHub](https://github.com/riviya/keypilot) · [DockerHub](https://hub.docker.com/r/riviyaaa/keypilot) · [Issues](https://github.com/riviya/keypilot/issues) · [Discussions](https://github.com/riviya/keypilot/discussions)

</div>