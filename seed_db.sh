#!/usr/bin/env bash
# Database Initialization Script for Distributed Banking System
set -e

DB_USER="${DB_USER:-bankapp}"
DB_PASSWORD="${DB_PASSWORD:-bankapp123}"
DB_NAME="${DB_NAME:-distributed_banking}"

echo "=========================================================="
echo "Initializing MySQL Database: $DB_NAME"
echo "=========================================================="

mysql -u "$DB_USER" -p"$DB_PASSWORD" "$DB_NAME" < src/main/resources/schema.sql
mysql -u "$DB_USER" -p"$DB_PASSWORD" "$DB_NAME" < src/main/resources/data.sql

echo "Database schema and seed records successfully initialized!"
mysql -u "$DB_USER" -p"$DB_PASSWORD" "$DB_NAME" -e "SELECT id, branch_code, branch_name, status, is_coordinator FROM branches; SELECT account_number, balance FROM accounts;"
