package com.distributed.banking.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "prepared_reservations")
public class PreparedReservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "global_transaction_id", nullable = false, length = 64)
    private String globalTransactionId;

    @Column(name = "account_number", nullable = false, length = 30)
    private String accountNumber;

    @Column(name = "reservation_type", nullable = false, length = 30)
    private String reservationType; // DEBIT_RESERVATION, CREDIT_RESERVATION

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 20)
    private String status; // PREPARED, COMMITTED, RELEASED

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public PreparedReservation() {}

    public PreparedReservation(String globalTransactionId, String accountNumber, String reservationType,
                               BigDecimal amount, String status, LocalDateTime expiresAt) {
        this.globalTransactionId = globalTransactionId;
        this.accountNumber = accountNumber;
        this.reservationType = reservationType;
        this.amount = amount;
        this.status = status;
        this.expiresAt = expiresAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getGlobalTransactionId() { return globalTransactionId; }
    public void setGlobalTransactionId(String globalTransactionId) { this.globalTransactionId = globalTransactionId; }

    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }

    public String getReservationType() { return reservationType; }
    public void setReservationType(String reservationType) { this.reservationType = reservationType; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
