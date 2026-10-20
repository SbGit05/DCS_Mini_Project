-- Distributed Banking System Schema
-- Clean database initialization for MySQL

DROP TABLE IF EXISTS replication_audit_log;
DROP TABLE IF EXISTS lamport_event_log;
DROP TABLE IF EXISTS prepared_reservations;
DROP TABLE IF EXISTS two_phase_transactions;
DROP TABLE IF EXISTS transactions;
DROP TABLE IF EXISTS accounts;
DROP TABLE IF EXISTS branches;
DROP TABLE IF EXISTS users;

-- 1. Users table (Customers and Administrators)
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    role VARCHAR(20) NOT NULL, -- ROLE_CUSTOMER, ROLE_ADMIN
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. Bank Branches (Nodes in distributed system)
CREATE TABLE branches (
    id BIGINT PRIMARY KEY,
    branch_code VARCHAR(10) NOT NULL UNIQUE,
    branch_name VARCHAR(100) NOT NULL,
    location VARCHAR(100) NOT NULL,
    rmi_host VARCHAR(100) NOT NULL DEFAULT 'localhost',
    rmi_port INT NOT NULL DEFAULT 1099,
    rmi_service_name VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ONLINE', -- ONLINE, UNAVAILABLE, RECOVERING
    is_coordinator BOOLEAN NOT NULL DEFAULT FALSE,
    last_heartbeat TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3. Bank Accounts
CREATE TABLE accounts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_number VARCHAR(30) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    branch_id BIGINT NOT NULL,
    account_type VARCHAR(20) NOT NULL DEFAULT 'SAVINGS',
    balance DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, FROZEN, CLOSED
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_account_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_account_branch FOREIGN KEY (branch_id) REFERENCES branches (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 4. Transactions table
CREATE TABLE transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    transaction_ref VARCHAR(64) NOT NULL UNIQUE,
    idempotency_key VARCHAR(64) UNIQUE,
    source_account_number VARCHAR(30),
    destination_account_number VARCHAR(30),
    transaction_type VARCHAR(30) NOT NULL, -- DEPOSIT, WITHDRAWAL, TRANSFER_LOCAL, TRANSFER_DISTRIBUTED
    amount DECIMAL(15, 2) NOT NULL,
    origin_branch_id BIGINT NOT NULL,
    destination_branch_id BIGINT,
    status VARCHAR(20) NOT NULL, -- SUCCESS, FAILED, PENDING, ABORTED
    failure_reason VARCHAR(255),
    lamport_timestamp BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_source_acc (source_account_number),
    INDEX idx_dest_acc (destination_account_number),
    INDEX idx_idempotency (idempotency_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 5. Two-Phase Commit (2PC) Transactions Log
CREATE TABLE two_phase_transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    global_transaction_id VARCHAR(64) NOT NULL UNIQUE,
    source_account VARCHAR(30) NOT NULL,
    dest_account VARCHAR(30) NOT NULL,
    amount DECIMAL(15, 2) NOT NULL,
    source_branch_id BIGINT NOT NULL,
    dest_branch_id BIGINT NOT NULL,
    coordinator_node_id BIGINT NOT NULL,
    phase1_source_vote VARCHAR(20) DEFAULT 'PENDING', -- VOTE_COMMIT, VOTE_ABORT, PENDING, TIMEOUT
    phase1_dest_vote VARCHAR(20) DEFAULT 'PENDING',
    decision VARCHAR(20) DEFAULT 'IN_PROGRESS', -- GLOBAL_COMMIT, GLOBAL_ABORT, IN_PROGRESS
    state VARCHAR(20) NOT NULL DEFAULT 'INITIATED', -- INITIATED, PREPARED, COMMITTED, ABORTED
    lamport_timestamp BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_gtx (global_transaction_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 6. Prepared Reservations for 2PC Phase 1
CREATE TABLE prepared_reservations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    global_transaction_id VARCHAR(64) NOT NULL,
    account_number VARCHAR(30) NOT NULL,
    reservation_type VARCHAR(30) NOT NULL, -- DEBIT_RESERVATION, CREDIT_RESERVATION
    amount DECIMAL(15, 2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PREPARED', -- PREPARED, COMMITTED, RELEASED
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_prep_gtx (global_transaction_id),
    INDEX idx_prep_acc (account_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 7. Lamport Event Log for Distributed Event Partial Ordering
CREATE TABLE lamport_event_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    node_id BIGINT NOT NULL,
    node_name VARCHAR(50) NOT NULL,
    logical_timestamp BIGINT NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    message TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_logical_ts (logical_timestamp)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 8. Replication and Audit Log across Branches
CREATE TABLE replication_audit_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sequence_number BIGINT NOT NULL,
    source_branch_id BIGINT NOT NULL,
    target_branch_id BIGINT NOT NULL,
    transaction_ref VARCHAR(64) NOT NULL,
    account_number VARCHAR(30) NOT NULL,
    change_type VARCHAR(30) NOT NULL, -- BALANCE_UPDATE, AUDIT_RECORD
    amount DECIMAL(15, 2) NOT NULL,
    sync_status VARCHAR(30) NOT NULL DEFAULT 'SYNCED', -- SYNCED, PENDING_RECONCILIATION, STALE
    lamport_timestamp BIGINT NOT NULL,
    synced_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_rep_seq (sequence_number),
    INDEX idx_rep_target (target_branch_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
