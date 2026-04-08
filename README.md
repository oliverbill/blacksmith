# Blacksmith

An AI-powered multi-agent pipeline for automated code analysis and implementation in brownfield projects. Blacksmith exposes itself as an **MCP (Model Context Protocol) server**, allowing you to trigger full AI-driven development cycles directly from a compatible client (e.g. Claude Desktop).

## How it works

Blacksmith runs a three-step sequential pipeline (via Spring Batch) when a run is triggered:

```
Constitution Agent → Architect Agent → Developer Agent
```

| Step | Agent | Responsibility |
|------|-------|----------------|
| 1 | **Constitution** | Clones the tenant's Git repos and produces a structured technical analysis of the codebase (stack, patterns, conventions, quality gaps). |
| 2 | **Architect** | Reads the constitution and the issue spec, then produces a change management plan with ordered, atomic tasks. |
| 3 | **Developer** | Executes each task from the architect's plan — generating or modifying files in the codebase. |

Each agent calls an LLM with a curated system prompt (in `src/main/resources/prompts/`) and uses bash tools to read and navigate the cloned repos.

## MCP Tools

Blacksmith exposes two tools via MCP that a client can call:

| Tool | Description |
|------|-------------|
| `findAllTenants` | Lists all registered tenants and their IDs. |
| `createRun(tenantId, title, spec, type, fullSyncRepo)` | Starts a pipeline run for a tenant. `type` is the issue type (feature, bug fix, tech debt). `fullSyncRepo` re-clones the repos when true. |

## Tech Stack

- **Java 21** / **Spring Boot 3.5**
- **Spring AI 1.1.3** — LLM client abstraction, MCP server
- **Spring Batch** — pipeline orchestration
- **Spring Data JPA** / **PostgreSQL** — persistence
- **Lombok** — boilerplate reduction

### LLM Providers

Each agent uses a priority-ordered list of providers with automatic fallback on rate limits:

1. **MiniMax** (primary)
2. **OpenRouter** (fallback)
3. **Anthropic Claude** (fallback)

## Getting Started

### Prerequisites

- Java 21+
- Maven
- PostgreSQL database
- API keys for at least one LLM provider

### Configuration

Copy the example below into `src/main/resources/application.properties` and fill in your own values. **Never commit real credentials.**

```properties
spring.application.name=blacksmith
server.port=8082

# Database
spring.datasource.url=jdbc:postgresql://<host>:5432/postgres
spring.datasource.username=<db-user>
spring.datasource.password=<db-password>

# LLM providers
spring.ai.anthropic.api-key=<anthropic-key>
spring.ai.anthropic.chat.options.model=claude-haiku-4-5-20251001

spring.ai.openai.api-key=<openai-or-github-models-key>
spring.ai.openai.base-url=https://models.github.ai/inference
spring.ai.openai.chat.options.model=gpt-4o

spring.ai.minimax.api-key=<minimax-key>
spring.ai.minimax.base-url=https://api.minimaxi.chat
spring.ai.minimax.chat.options.model=MiniMax-M2.7

spring.ai.openrouter.api-key=<openrouter-key>
spring.ai.openrouter.base-url=https://openrouter.ai/api
spring.ai.openrouter.chat.options.model=minimax/minimax-m2.5:free

# Pipeline
blacksmith.tenant.repo.basefolder=/tmp/blacksmith/repos

# MCP server
spring.ai.mcp.server.name=blacksmith
spring.ai.mcp.server.version=1.0.0
spring.ai.mcp.server.type=SYNC
spring.ai.mcp.server.sse-message-endpoint=/mcp/messages

# Batch
spring.batch.jdbc.initialize-schema=always
spring.batch.job.enabled=false

# JPA
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
```

### Build & Run

```bash
./mvnw clean package
./mvnw spring-boot:run
```

The server starts on port `8082`.

### MCP Server endpoint

```
http://localhost:8082/mcp/messages
```

Add this to your MCP client config (e.g. Claude Desktop `claude_desktop_config.json`) to connect.

## Project Structure

```
src/main/java/com/oliversoft/blacksmith/
├── agent/          # BlackSmithAgent — LLM invocation, fallback, JSON parsing
├── batch/          # Spring Batch pipeline (Constitution → Architect → Developer steps)
├── config/         # LLM and MCP configuration beans
├── controller/     # MCP server tool definitions (BlacksmithMcpServer)
├── core/           # ContextBuilder, GitCloner
├── model/
│   ├── dto/        # Agent input/output DTOs
│   ├── entity/     # JPA entities (Tenant, TenantRun, TaskExecution, RunArtifact)
│   └── enumeration/
├── persistence/    # Spring Data repositories
├── router/         # LLMRouter — per-agent provider priority list
└── tool/           # BashTools — tools exposed to agents (list files, grep, read, etc.)

src/main/resources/prompts/
├── constitution-agent.md
├── architect-agent.md
└── developer-agent.md
```

## Data Model

| Entity | Purpose |
|--------|---------|
| `Tenant` | A project/team. Holds Git repo URLs and an optional constitution manual. |
| `TenantRun` | A single pipeline execution for a tenant, tied to an issue spec. |
| `TaskExecution` | An individual task from the architect's plan and its execution status. |
| `RunArtifact` | Files generated or modified by the developer agent during a run. |
