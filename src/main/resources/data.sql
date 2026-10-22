-- Initial Seed Data for Distributed Banking System

-- 1. Branches (Nodes)
INSERT INTO branches (id, branch_code, branch_name, location, rmi_host, rmi_port, rmi_service_name, status, is_coordinator)
VALUES
(1, 'PUN01', 'Pune Central Branch', 'Pune, Maharashtra', 'localhost', 1099, 'BranchService_1', 'ONLINE', FALSE),
(2, 'MUM02', 'Mumbai Main Branch', 'Mumbai, Maharashtra', 'localhost', 1099, 'BranchService_2', 'ONLINE', FALSE),
(3, 'DEL03', 'Delhi Regional Hub', 'New Delhi', 'localhost', 1099, 'BranchService_3', 'ONLINE', TRUE);

-- 2. Users (Password for admin: admin123, Password for customers: user123)
INSERT INTO users (id, username, password_hash, full_name, email, role)
VALUES
(1, 'admin', '$2b$10$nR511AjEd9kYwxDjSPik2O0ETJ4NreRNLOfAPZ2QvjW7aKnZvoUnq', 'System Administrator', 'admin@distributedbank.internal', 'ROLE_ADMIN'),
(2, 'shreedhar', '$2b$10$xhQrtwnzNs9B13Tdcx5gbeSASdgCptOnxKlWoJsmq4nSmzSiXSetq', 'Shreedhar Kulkarni', 'shreedhar@example.com', 'ROLE_CUSTOMER'),
(3, 'aditi', '$2b$10$xhQrtwnzNs9B13Tdcx5gbeSASdgCptOnxKlWoJsmq4nSmzSiXSetq', 'Aditi Sharma', 'aditi@example.com', 'ROLE_CUSTOMER'),
(4, 'rahul', '$2b$10$xhQrtwnzNs9B13Tdcx5gbeSASdgCptOnxKlWoJsmq4nSmzSiXSetq', 'Rahul Verma', 'rahul@example.com', 'ROLE_CUSTOMER'),
(5, 'priya', '$2b$10$xhQrtwnzNs9B13Tdcx5gbeSASdgCptOnxKlWoJsmq4nSmzSiXSetq', 'Priya Patel', 'priya@example.com', 'ROLE_CUSTOMER'),
(6, 'vikram', '$2b$10$xhQrtwnzNs9B13Tdcx5gbeSASdgCptOnxKlWoJsmq4nSmzSiXSetq', 'Vikram Malhotra', 'vikram@example.com', 'ROLE_CUSTOMER'),
(7, 'neha', '$2b$10$xhQrtwnzNs9B13Tdcx5gbeSASdgCptOnxKlWoJsmq4nSmzSiXSetq', 'Neha Sharma', 'neha@example.com', 'ROLE_CUSTOMER');

-- 3. Accounts
INSERT INTO accounts (id, account_number, user_id, branch_id, account_type, balance, status, version)
VALUES
(1, 'PUN-1001', 2, 1, 'SAVINGS', 25000.00, 'ACTIVE', 0),
(2, 'PUN-1002', 3, 1, 'SAVINGS', 15000.00, 'ACTIVE', 0),
(3, 'MUM-2001', 4, 2, 'SAVINGS', 30000.00, 'ACTIVE', 0),
(4, 'MUM-2002', 5, 2, 'SAVINGS', 18000.00, 'ACTIVE', 0),
(5, 'DEL-3001', 6, 3, 'SAVINGS', 35000.00, 'ACTIVE', 0),
(6, 'DEL-3002', 7, 3, 'SAVINGS', 20000.00, 'ACTIVE', 0);

-- 4. Initial Lamport Clock Events
INSERT INTO lamport_event_log (node_id, node_name, logical_timestamp, event_type, message)
VALUES
(1, 'Pune Central Branch', 1, 'NODE_INIT', 'Pune Central Branch (Node 1) initialized on RMI port 1099'),
(2, 'Mumbai Main Branch', 2, 'NODE_INIT', 'Mumbai Main Branch (Node 2) initialized on RMI port 1099'),
(3, 'Delhi Regional Hub', 3, 'COORDINATOR_ANNOUNCE', 'Delhi Regional Hub (Node 3) elected as initial 2PC transaction coordinator');
