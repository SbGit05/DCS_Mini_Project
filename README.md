# Distributed Banking System

**“Design and Implementation of a Distributed Banking System with Transaction Consistency, Branch Synchronization, and Fault Tolerance”**

A full-scale, fully functional, and rigorously verified Distributed Banking System engineered in Java for the **Distributed Computing Systems Laboratory** curriculum.

---

## 🏛️ System Architecture Overview

The system interconnects independent bank branch nodes over an authentic **Java Remote Method Invocation (RMI)** distributed middleware layer, enforcing ACID consistency, distributed concurrency control, and fault tolerance:

```
+-------------------------------------------------------------------------------+
|                           CLIENT PRESENTATION LAYER                           |
|       Customer Portal (customer.html)   |   Admin Control Centre (admin.html) |
+---------------------------------------+---------------------------------------+
                                        | HTTP / JSON REST APIs (Port 8085)
+---------------------------------------v---------------------------------------+
|                       APPLICATION GATEWAY & REST CONTROLLERS                  |
|    AuthController  |  CustomerController  |  AdminController  |  DistController|
+---------------------------------------+---------------------------------------+
                                        | Java RMI Remote Invocations (Port 1099)
+---------------------------------------v---------------------------------------+
|                    DISTRIBUTED RMI SERVICE & COORDINATION LAYER               |
|                                                                               |
|   +-----------------------+  2PC Protocol   +-----------------------+         |
|   | Pune Branch (Node 1)  |<===============>| Mumbai Branch (Node 2)|         |
|   | Port: 1099 / Service_1|                 | Port: 1099 / Service_2|         |
|   +-----------------------+                 +-----------------------+         |
|               ^                                         ^                     |
|               |              Bully Election             |                     |
|               +-------------------+---------------------+                     |
|                                   |                                           |
|                     +---------------------------+                             |
|                     | Delhi Regional Hub (Node 3)|                            |
|                     | Active 2PC Coordinator    |                             |
|                     +---------------------------+                             |
+---------------------------------------+---------------------------------------+
                                        | JDBC / HikariCP
+---------------------------------------v---------------------------------------+
|                           PERSISTENCE LAYER (MySQL InnoDB)                    |
|    accounts | transactions | two_phase_transactions | lamport_event_log ...   |
+-------------------------------------------------------------------------------+
```

---

## 📚 Academic Syllabus Mapping

| Syllabus Unit | Theoretical Concept | Project Implementation |
|---|---|---|
| **Unit 1: Introduction** | Client-server architecture, distributed middleware, layered architecture | Web UI → Spring Boot Gateway → Java RMI remote objects (`BranchServiceRemote`) → MySQL InnoDB persistence. |
| **Unit 2: Communication & Coordination** | Java RMI, logical clocks, Lamport timestamps, mutual exclusion, Bully election | Real RMI registry (`1099`), `LamportClock` causal tracking piggybacked on messages, `BullyElectionService` with `ELECTION`, `OK`, `COORDINATOR` messaging. |
| **Unit 3: Naming & Storage** | Unique naming, request routing, persistent transaction logs, idempotency | Branch-prefixed accounts (`PUN-1001`, `MUM-2001`), RMI service binding, `idempotency_key` deduplication, atomic database logging. |
| **Unit 4: Consistency & Fault Tolerance** | Concurrency control, 2PC commit protocol, replication management, failure recovery | `SELECT ... FOR UPDATE` row locks, `TwoPhaseCommitCoordinator` (Prepare & Commit/Abort), `ReplicationManager` reconciliation, heartbeat monitor. |

---

## 🚀 Key Implemented Features

1. **Authentication & User Management**:
   - Customer registration and login with OpenBSD **BCrypt** password hashing (`$2a$10$...`).
   - Role-based authorization (`ROLE_CUSTOMER`, `ROLE_ADMIN`) with secure UUID session tokens.
   - Account ownership validation preventing cross-customer access.

2. **Atomic Banking Operations**:
   - Balance inquiry, cash deposit, and cash withdrawal using exact `BigDecimal` currency calculations.
   - **Pessimistic Row-Level Locking** (`@Lock(LockModeType.PESSIMISTIC_WRITE)` generating `SELECT ... FOR UPDATE` in MySQL InnoDB).
   - Proven race condition prevention during simultaneous withdrawals.
   - Deterministic lexicographical lock ordering on local transfers to eliminate deadlocks.
   - Idempotency key handling to prevent duplicate processing on client retry.

3. **Two-Phase Commit (2PC) Distributed Protocol**:
   - Automatic routing of cross-branch transfers (e.g. Pune `PUN-1001` to Mumbai `MUM-2001`) through the 2PC Coordinator.
   - **Phase 1 (Prepare)**: Coordinator issues prepare requests; source branch locks funds with a `PREPARED` reservation; participants vote `VOTE_COMMIT` or `VOTE_ABORT`.
   - **Phase 2 (Commit or Abort)**: If both vote commit, coordinator broadcasts `GLOBAL_COMMIT`; if either votes abort or times out, coordinator broadcasts `GLOBAL_ABORT` and reservations are released.

4. **Distributed Coordination & Ordering**:
   - **Lamport Logical Clocks**: Implements $L_i = L_i + 1$ on local events and $L_j = \max(L_j, L_{msg}) + 1$ on message receipts. Every event is tagged with its logical timestamp $L(e)$.
   - **Bully Leader Election Algorithm**: When coordinator failure is simulated, the initiating node messages higher-ID nodes; if none respond, it claims leadership and broadcasts `COORDINATOR`.

5. **Branch Replication & Fault Tolerance**:
   - Authoritative vs. Replica separation: Account balances are strongly consistent at the home branch; peer branches receive asynchronous replica audit records.
   - Heartbeat monitoring every 5s detecting `ONLINE`, `UNAVAILABLE`, and `RECOVERING` states.
   - Fault injection: Administrators can simulate branch crashing/stopping and test automated recovery reconciliation.

6. **Responsive Web Portals**:
   - **Customer Portal**: Account overview, balance card, deposit/withdrawal forms, fund transfer with automatic 2PC detection, live passbook with Lamport timestamps.
   - **Administrator Console**: Live cluster topology grid, node fault injection buttons, interactive Bully election runner with step trace, 2PC live monitor, replication audit table, and Lamport event log.

---

## ⚙️ Prerequisites

- **Java Development Kit (JDK)**: Java 17 LTS or higher (tested on OpenJDK 25/26).
- **Apache Maven**: Version 3.8+
- **MySQL Database Server**: Version 8.0+ or 9.0+
- **Python 3**: For generating project documentation (`reportlab`, `python-docx` installed).

---

## 🛠️ Setup and Installation

### 1. Database Configuration
Ensure MySQL is running on localhost port 3306. Create the database and user:
```bash
mysql -u root -p -e "
CREATE DATABASE IF NOT EXISTS distributed_banking CHARACTER SET utf8mb4;
CREATE USER IF NOT EXISTS 'bankapp'@'localhost' IDENTIFIED BY 'bankapp123';
GRANT ALL PRIVILEGES ON distributed_banking.* TO 'bankapp'@'localhost';
FLUSH PRIVILEGES;
"
```

Initialize the schema and seed data using the provided script:
```bash
./seed_db.sh
```

### 2. Configuration Settings
Settings are pre-configured in `src/main/resources/application.properties` and `.env.example`:
- Database URL: `jdbc:mysql://localhost:3306/distributed_banking`
- Database User / Password: `bankapp` / `bankapp123`
- Web Portal Port: `8085`
- Java RMI Registry Port: `1099`

---

## 🧪 Running Automated Verification Tests

Execute all 13 automated test suites verifying all 18 requirements:
```bash
./run_tests.sh
# or: mvn test
```

### Verified Test Scenarios:
1. `BankingCoreIntegrationTest`: Customer registration, BCrypt authentication, deposits, withdrawals, local transfers, insufficient funds rejection, invalid amount handling, idempotency deduplication.
2. `ConcurrencyAndLockingTest`: Multi-threaded simultaneous withdrawals test with 10 threads on an account with ₹5,000 balance. Exactly 5 succeed, 5 fail, and the final balance is exactly ₹0.00.
3. `LamportClockAndOrderingTest`: Monotonic local ticks and causal receive updates ($L_{send} < L_{recv}$).
4. `TwoPhaseCommitTest`: Successful cross-branch commit path and insufficient-funds abort path.
5. `BullyElectionTest`: Normal election (Node 3 wins) and failover election (Node 2 wins when Node 3 is offline).
6. `ReplicationAndFaultToleranceTest`: Node failure simulation, pending replica queue, and reconnected reconciliation.
7. `WebControllerIntegrationTest`: Full end-to-end REST API flows for customer and administrator.

---

## 💻 Running the Application

Start the application using the startup script:
```bash
./run_app.sh
```

Open your browser and navigate to:
**`http://localhost:8085/`**

### Pre-Configured Demo Accounts:
- **Customer 1 (Pune Central)**: `shreedhar` / `user123` (Account: `PUN-1001`)
- **Customer 2 (Pune Central)**: `aditi` / `user123` (Account: `PUN-1002`)
- **Customer 3 (Mumbai Main)**: `rahul` / `user123` (Account: `MUM-2001`)
- **Customer 4 (Mumbai Main)**: `priya` / `user123` (Account: `MUM-2002`)
- **Administrator**: `admin` / `admin123`

---

## 📄 Final Project Documentation Deliverables

The workspace includes generated documentation covering all 35 required sections, architecture diagrams, live screenshots, and viva questions:
- **`Distributed_Banking_System_Project_Documentation.pdf`** (5.4 MB report with embedded screenshots)
- **`Distributed_Banking_System_Project_Documentation.docx`** (1.6 MB editable Word document)
- **`generate_docs.py`**: Python script to regenerate documentation artifacts anytime.

---

## 🎓 Laboratory Viva Questions & Answers

### Q1: What is Java RMI and how does it function in this banking system?
**A:** Java Remote Method Invocation (RMI) allows an object on one JVM to invoke methods on an object living in another JVM across a network. In our system, the RMI Registry runs on port 1099. Each branch exports a `BranchServiceRemote` object (`BranchService_1`, `BranchService_2`, `BranchService_3`). When an inter-branch transfer occurs, the 2PC coordinator invokes `prepareTransaction()` and `commitTransaction()` across these genuine RMI stubs over TCP.

### Q2: How does the system prevent the double-spending problem during simultaneous withdrawals?
**A:** Concurrency control is enforced via database row-level locking using `@Lock(LockModeType.PESSIMISTIC_WRITE)`, which issues `SELECT ... FOR UPDATE` in MySQL InnoDB. When two threads concurrently attempt to withdraw from the same account, the first thread acquires an exclusive row lock. The second thread is blocked until the first transaction commits or rolls back.

### Q3: Explain the Two-Phase Commit (2PC) protocol used for cross-branch transfers.
**A:**
1. **Phase 1 (Prepare)**: The coordinator generates a global transaction ID (`GTX-<uuid>`) and sends `PrepareRequest` to both source and target branches. The source branch places a temporary `PREPARED` reservation on the balance. Both branches vote `VOTE_COMMIT` (or `VOTE_ABORT` if funds are insufficient).
2. **Phase 2 (Commit/Abort)**: If both vote commit, the coordinator broadcasts `GLOBAL_COMMIT`, and both branches finalize the debits/credits. If either branch votes abort, the coordinator broadcasts `GLOBAL_ABORT` and all reservations are released.

### Q4: Why are Lamport logical clocks needed in distributed banking?
**A:** Physical quartz clocks drift over time and cannot guarantee exact synchronization across distributed network nodes. Lamport logical clocks provide a monotonically increasing counter that ensures causal partial ordering: if event A causes event B (such as sending an RMI request and receiving its response), $L(A) < L(B)$ is mathematically guaranteed.

### Q5: How does the Bully algorithm operate upon coordinator failure?
**A:** Each node has a unique numeric ID (Node 1: Pune, Node 2: Mumbai, Node 3: Delhi). When coordinator failure is detected, a node sends `ELECTION` messages to all nodes with higher IDs. If no higher node responds, the initiator proclaims itself leader and broadcasts `COORDINATOR` to all nodes. If a higher node responds `OK`, the higher node takes over the election. In our cluster, Node 3 wins under normal conditions; if Node 3 is offline, Node 2 wins.
