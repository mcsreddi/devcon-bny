package com.bny.spark26.lab.model;

import java.math.BigDecimal;
import java.time.Instant;

public class Order {
    private String id;
    private String customerId;
    private String productId;
    private int quantity;
    private BigDecimal totalAmount;
    private String status;
    private Instant createdAt;

    public Order(String id, String customerId, String productId, int quantity, BigDecimal totalAmount, String status) {
        this.id = id;
        this.customerId = customerId;
        this.productId = productId;
        this.quantity = quantity;
        this.totalAmount = totalAmount;
        this.status = status;
        this.createdAt = Instant.now();
    }

    // Getters
    public String getId() { return id; }
    public String getCustomerId() { return customerId; }
    public String getProductId() { return productId; }
    public int getQuantity() { return quantity; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public String getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }

    // Setters
    public void setStatus(String status) { this.status = status; }
}
