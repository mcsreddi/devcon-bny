package com.bny.spark26.lab.model;

public class InventoryResponse {
    private String productId;
    private int availableStock;
    private boolean available;

    public InventoryResponse() {}

    public InventoryResponse(String productId, int availableStock, boolean available) {
        this.productId = productId;
        this.availableStock = availableStock;
        this.available = available;
    }

    public String getProductId() { return productId; }
    public int getAvailableStock() { return availableStock; }
    public boolean isAvailable() { return available; }
    public void setProductId(String productId) { this.productId = productId; }
    public void setAvailableStock(int availableStock) { this.availableStock = availableStock; }
    public void setAvailable(boolean available) { this.available = available; }
}
