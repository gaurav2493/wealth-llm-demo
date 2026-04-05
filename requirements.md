# Wealth Management Platform — Requirements

## Overview

A web-based wealth management platform where an admin can log in and manage clients, schemes, transactions, and view holdings. The system consists of a plain HTML/JS/CSS front-end and a Kotlin/Maven back-end backed by PostgreSQL.

---

## Tech Stack

| Layer | Technology |
|----------|----------------------------------------------|
| Frontend | Plain HTML, JavaScript (vanilla), CSS |
| Backend | Kotlin, Maven, JVM 25 |
| Database | PostgreSQL 18 |

---

## Actors

- Admin — the sole user role. Authenticates via login and performs all management operations.

---

## Functional Requirements

### 1. Authentication

- Admin can log in with username and password.
- Sessions are maintained server-side (cookie or token-based).
- Unauthenticated requests to protected endpoints are rejected.

### 2. Scheme Management

| Operation | Description |
|-----------|-----------------------------------------------|
| Add | Admin can create a new scheme (name, type, NAV, etc.) |
| Delete | Admin can delete an existing scheme |
| List | Admin can view all schemes |

### 3. Client Management

| Operation | Description |
|-----------|-----------------------------------------------|
| Add | Admin can register a new client (name, email, phone, etc.) |
| Delete | Admin can remove a client |
| List | Admin can view all clients |

### 4. Transaction Management

| Operation | Description |
|-----------|-----------------------------------------------|
| Add | Admin can record a transaction (client, scheme, units, amount, date, type: buy/sell) |
| Delete | Admin can delete a transaction |
| List | Admin can list all transactions |

### 5. Holdings View

- Holdings are derived from transactions (aggregated units per client per scheme, valued at current NAV).
- Admin can view a holdings table showing client name, scheme name, units held, and holding value.
- Filtering:
  - By client
  - By scheme
- Sorting:
  - By holding value (ascending / descending)

---

## Pages / Screens

1. Login
2. Dashboard (landing page after login)
3. Schemes — list, add, delete
4. Clients — list, add, delete
5. Transactions — list, add, delete
6. Holdings — list with filters and sort

---

## API Endpoints (REST)

### Auth
- `POST /api/auth/login`
- `POST /api/auth/logout`

### Schemes
- `GET /api/schemes`
- `POST /api/schemes`
- `DELETE /api/schemes/{id}`

### Clients
- `GET /api/clients`
- `POST /api/clients`
- `DELETE /api/clients/{id}`

### Transactions
- `GET /api/transactions`
- `POST /api/transactions`
- `DELETE /api/transactions/{id}`

### Holdings
- `GET /api/holdings?clientId=&schemeId=&sortBy=value&sortOrder=asc`

---

## Database Tables

### admin
| Column | Type |
|--------------|--------------|
| id | SERIAL PK |
| username | VARCHAR |
| password_hash| VARCHAR |

### client
| Column | Type |
|----------|--------------|
| id | SERIAL PK |
| name | VARCHAR |
| email | VARCHAR |
| phone | VARCHAR |

### scheme
| Column | Type |
|----------|--------------|
| id | SERIAL PK |
| name | VARCHAR |
| type | VARCHAR |
| nav | DECIMAL |

### transaction
| Column | Type |
|------------|--------------|
| id | SERIAL PK |
| client_id | FK → client |
| scheme_id | FK → scheme |
| type | VARCHAR (buy/sell) |
| units | DECIMAL |
| amount | DECIMAL |
| date | DATE |

Holdings are computed at query time from transactions (no separate table).

---

## Non-Functional Requirements

- Backend serves the static frontend files.
- No frontend frameworks or build tools — plain files only.
- All API responses use JSON.
- Passwords are stored hashed (e.g., bcrypt).
- Basic input validation on both client and server side.
