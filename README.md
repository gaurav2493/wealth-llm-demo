# Wealth Management Platform

![CI](https://github.com/gaurav2493/wealth-llm-demo/actions/workflows/ci.yml/badge.svg)
![Coverage](.github/badges/jacoco.svg)
![Branches](.github/badges/branches.svg)

A web-based wealth management system for managing clients, investment schemes, transactions, and holdings. Includes PDF report generation and an MCP (Model Context Protocol) server for AI assistant integration.

## Tech Stack

| Component | Technology |
|-----------|-----------|
| Backend | Kotlin, Spring Boot 3.4, Maven, JVM 25 |
| Database | PostgreSQL |
| Frontend | Plain HTML, JavaScript, CSS |
| MCP Server | Kotlin, Java MCP SDK 1.1.1, stdio transport |

## Project Structure

```
backend/                          # Spring Boot application
├── src/main/kotlin/com/wealthmgmt/
│   ├── Application.kt            # Entry point
│   ├── config/SecurityConfig.kt  # Auth & session config
│   ├── controller/               # REST controllers
│   ├── model/Models.kt           # Data classes
│   ├── repository/               # JDBC repositories
│   └── service/ReportService.kt  # PDF report generation
├── src/main/resources/
│   ├── static/                   # Frontend (HTML/JS/CSS)
│   ├── schema.sql                # DB schema
│   └── application.properties
└── src/test/                     # Integration tests (H2)

mcp-server/                       # Standalone MCP server
├── src/main/kotlin/com/wealthmgmt/mcp/
│   ├── McpServer.kt              # Tool definitions & server setup
│   └── ApiClient.kt              # HTTP client to backend API
└── pom.xml
```

## Prerequisites

- JVM 25
- Maven 3.9+
- PostgreSQL running on localhost:5432

## Running the Application

### 1. Create the database

```bash
psql -h localhost -d template1 -c "CREATE DATABASE wealthmgmt;"
```

### 2. Update database credentials

Edit `backend/src/main/resources/application.properties` and set your PostgreSQL username/password.

### 3. Build and run the backend

```bash
cd backend
mvn package -DskipTests
java -jar target/wealth-management-1.0.0.jar
```

The app starts at http://localhost:8080. Log in with `admin` / `admin123`.

### 4. Run tests

```bash
cd backend
mvn test
```

Tests use an H2 in-memory database, so no PostgreSQL is needed for testing.

## Features

- **Authentication** — session-based login/logout
- **Client Management** — add, list, delete clients
- **Scheme Management** — add, list, delete investment schemes (with NAV)
- **Transaction Management** — record buy/sell transactions, list, delete
- **Holdings View** — aggregated holdings computed from transactions, filterable by client, scheme, as-of date, sortable by value
- **PDF Reports** — downloadable per-client reports:
  - Holdings Report
  - Transaction Report
  - Capital Gains Report (FIFO-based)

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | /api/auth/login | Login |
| POST | /api/auth/logout | Logout |
| GET/POST/DELETE | /api/clients | Client CRUD |
| GET/POST/DELETE | /api/schemes | Scheme CRUD |
| GET/POST/DELETE | /api/transactions | Transaction CRUD |
| GET | /api/holdings | Holdings with filters |
| GET | /api/reports/holdings/{clientId} | Holdings PDF |
| GET | /api/reports/transactions/{clientId} | Transactions PDF |
| GET | /api/reports/capital-gains/{clientId} | Capital Gains PDF |

## MCP Server

The MCP (Model Context Protocol) server allows AI assistants like Kiro to query the wealth management data through natural language. It runs as a separate JVM process communicating over stdio.

### How it works

```
┌─────────┐   stdio (JSON-RPC)   ┌────────────┐   HTTP/REST   ┌─────────┐
│  Kiro /  │ ◄──────────────────► │ MCP Server │ ────────────► │ Spring  │
│ AI Agent │                      │  (Kotlin)  │               │  Boot   │
└─────────┘                       └────────────┘               └─────────┘
```

1. Kiro launches the MCP server as a child process
2. Communication happens over stdin/stdout using JSON-RPC (MCP protocol)
3. When a tool is called, the MCP server makes HTTP requests to the Spring Boot API
4. The MCP server authenticates once with admin credentials and reuses the session
5. Results are returned as JSON text back through stdio

### Available MCP Tools

| Tool | Description |
|------|-------------|
| `list_clients` | List all clients |
| `list_schemes` | List all investment schemes |
| `list_transactions` | List all transactions |
| `get_holdings` | Get all current holdings |
| `get_holdings_filtered` | Get holdings with optional filters (clientId, schemeId, sortBy, sortOrder, asOfDate) |

### Building the MCP server

```bash
cd mcp-server
mvn package
```

### Configuring in Kiro

The MCP server is configured in `.kiro/settings/mcp.json`:

```json
{
  "mcpServers": {
    "wealth-management": {
      "command": "java",
      "args": ["-jar", "<absolute-path>/mcp-server/target/wealth-mcp-server-1.0.0.jar"],
      "disabled": false
    }
  }
}
```

Update the path to match your workspace location. The backend must be running on port 8080 for the MCP server to work.
