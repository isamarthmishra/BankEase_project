# BankEase — NextGen Net Banking Platform

BankEase is an enterprise-grade full-stack digital banking web application engineered with **Spring Boot 4 / Java 21**, **MySQL 8**, **Spring Security 7**, and a responsive **Glassmorphism Fintech Single-Page Application (SPA)**.

---

## 🌟 Architectural Highlights

1. **Enterprise Banking Security**:
   - Industry-standard **BCrypt password hashing** with salted rounds (passwords are never stored in plain text).
   - **Stateless HMAC-SHA256 JWT Authentication** (24-hour expiration, tamper protection).
   - Strict ownership isolation: Customers can access, query, and transact only against accounts they legally own.
   - Password hashes are annotated with `@JsonProperty(access = WRITE_ONLY)` to prevent accidental leakage in JSON serialization.

2. **Atomic & Concurrency-Safe Financial Ledger**:
   - Fund transfers execute inside **`@Transactional(isolation = READ_COMMITTED)`** boundaries.
   - **Deterministic Pessimistic Locking (`SELECT ... FOR UPDATE`)**: Lock acquisition order is strictly sorted alphabetically by account number. This eliminates deadlocks when two accounts transfer money to each other simultaneously.
   - **Idempotency Key Protection**: Transfer requests support client-generated idempotency keys (`idempotencyKey`). Duplicate submissions return the original receipt and prevent double debits.
   - Automatic rollback on any transactional failure ensures zero ledger discrepancies.

3. **Full-Featured Core & NextGen Banking**:
   - **Accounts**: Savings, Checking, Current, and Fixed Deposit accounts with live balance inquiry and deposits.
   - **Transfers**: Instant inter-account fund transfers with official generated transaction receipts.
   - **Beneficiaries**: Payee management with 1-click quick transfer routing.
   - **Ledger & Audit Trail**: Real-time transaction history with printable/copyable receipt modal.
   - **Utility Bill Payments**: Instant payment for Electricity, Water, Cooking Gas, Broadband, and Credit Cards with account debit.
   - **Instant Loans**: Automated compound EMI calculation (Personal, Home, Auto, Education) with instant loan approval and direct principal disbursement into the customer's account.
   - **Wealth & Investments**: Fixed Deposits (7.25%), Mutual Funds (12.00%), Sovereign Bonds (8.10%), and Recurring Deposits (6.80%) with maturity projections and real-time liquidation into customer accounts.

---

## 📂 Project Structure

```text
BankEase/
├── backend/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/bankease/
│   │   │   │   ├── config/          # Spring Security, CORS, Authentication configuration
│   │   │   │   ├── controller/      # REST API Controllers (Auth, Accounts, Transfers, Bills, Loans, etc.)
│   │   │   │   ├── dto/             # Strongly typed request/response transfer objects with Bean Validation
│   │   │   │   ├── entity/          # JPA Domain Entities & Enums (Account, Transaction, Bill, Loan, Investment)
│   │   │   │   ├── exception/       # Domain exceptions & GlobalExceptionHandler
│   │   │   │   ├── repository/      # Spring Data JPA repositories with pessimistic lock queries
│   │   │   │   ├── security/        # JWT Service, Authentication filter, UserDetails service
│   │   │   │   └── service/         # Core transactional business logic
│   │   │   └── resources/
│   │   │       ├── application.properties
│   │   │       └── static/          # Responsive Glassmorphic Net Banking SPA
│   │   │           ├── index.html
│   │   │           ├── css/styles.css
│   │   │           ├── js/api.js
│   │   │           └── js/app.js
│   │   └── test/java/com/bankease/  # Comprehensive integration and concurrency test suite
│   ├── pom.xml
│   └── Dockerfile
├── docker-compose.yml               # Containerized deployment for MySQL and Backend
├── .env.example
└── README.md
```

---

## 🚀 Quickstart Guide

### Prerequisites
- **Java 21** or higher
- **MySQL 8.0+** running locally on port `3306` with database `bankease` (or Docker)
- **Maven Wrapper** (included as `./mvnw`)

---

### Step 1: Database Setup

Ensure MySQL 8.0+ is running locally and create the database:
```sql
CREATE DATABASE IF NOT EXISTS bankease;
```

#### Environment Variables & Database Configuration
BankEase supports standard environment variables to connect to your database securely without modifying source code. You can copy [.env.example](file:///.env.example) to `.env` or export variables in your shell:

| Environment Variable | Default (Local Dev) | Description |
|---|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:mysql://localhost:3306/bankease?...` | MySQL JDBC connection string |
| `SPRING_DATASOURCE_USERNAME` | `root` | Database username |
| `SPRING_DATASOURCE_PASSWORD` | *(Set via environment / `.env`)* | Database password |
| `JWT_SECRET` | *(256-bit secure key)* | HMAC-SHA256 signing secret |
| `SERVER_PORT` | `8081` | Web server port |

---

### Step 2: Running Automated Tests

Run the full test suite with all 33 automated integration tests (including the multithreaded concurrency test):

```bash
cd backend
./mvnw test
```

Expected output:
```text
[INFO] Tests run: 33, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

---

### Step 3: Run the Application Locally

```bash
cd backend
./mvnw spring-boot:run
```

Once started:
- **Net Banking Web Application**: Open [http://localhost:8081](http://localhost:8081) in your browser.
- **REST API Endpoints**: Base path at `http://localhost:8081/api/`.
- **OpenAPI Specification**: Available at [http://localhost:8081/api/docs/openapi.json](http://localhost:8081/api/docs/openapi.json).

---

### Step 4: Run with Docker Compose

Deploy the complete stack (MySQL 8 + Spring Boot application) with one command:

```bash
docker-compose up --build -d
```

---

### Demo Credentials & Automated Data Seeding
BankEase includes an automated, idempotent `DataSeeder` that runs on application startup. It guarantees that the documented demo account and its associated bank accounts, beneficiaries, and sample bills exist without resetting the database or overwriting existing user data:

- **Email**: `live.demo@bankease.com`
- **Password**: `SecurePassword@123`
- *(Alternatively, click "Create an Account" on the login page to register a fresh user with immediate access.)*

---

## 🔐 API Reference Overview

| HTTP Method | Endpoint | Description | Access |
|---|---|---|---|
| `POST` | `/api/auth/register` | Register customer profile | Public |
| `POST` | `/api/auth/login` | Login and obtain JWT token | Public |
| `GET` | `/api/users/me` | Fetch authenticated user profile | Bearer JWT |
| `GET` | `/api/accounts` | List accounts of logged-in user | Bearer JWT |
| `POST` | `/api/accounts` | Open new bank account | Bearer JWT |
| `GET` | `/api/accounts/{acc}` | Get account details | Bearer JWT |
| `GET` | `/api/accounts/{acc}/balance` | Real-time balance inquiry | Bearer JWT |
| `POST` | `/api/accounts/{acc}/deposit` | Deposit funds | Bearer JWT |
| `POST` | `/api/transfers` | Transfer funds (atomic & idempotent) | Bearer JWT |
| `GET` | `/api/transactions` | Full ledger transaction history | Bearer JWT |
| `GET` | `/api/transactions/account/{acc}` | Filter transactions by account number | Bearer JWT |
| `GET` | `/api/transactions/receipt/{ref}` | Get official transaction receipt | Bearer JWT |
| `GET` | `/api/beneficiaries` | List saved beneficiaries | Bearer JWT |
| `POST` | `/api/beneficiaries` | Add a beneficiary | Bearer JWT |
| `DELETE`| `/api/beneficiaries/{id}` | Delete a beneficiary | Bearer JWT |
| `GET` | `/api/bills` | List utility bills (optional `?status=`) | Bearer JWT |
| `POST` | `/api/bills` | Add new utility bill | Bearer JWT |
| `POST` | `/api/bills/{id}/pay` | Settle bill from account | Bearer JWT |
| `GET` | `/api/loans` | View active and repaid loans | Bearer JWT |
| `POST` | `/api/loans/apply` | Apply & auto-disburse loan into account | Bearer JWT |
| `POST` | `/api/loans/{id}/repay` | Pay loan monthly installment | Bearer JWT |
| `GET` | `/api/investments` | Portfolio valuation | Bearer JWT |
| `POST` | `/api/investments` | Create investment (FD, Mutual Fund, Bonds) | Bearer JWT |
| `POST` | `/api/investments/{id}/withdraw` | Liquidate investment with returns | Bearer JWT |
