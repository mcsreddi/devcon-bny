package com.bny.spark26.lab.service;

import com.bny.spark26.lab.client.InventoryClient;
import com.bny.spark26.lab.model.InventoryResponse;
import com.bny.spark26.lab.model.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final InventoryClient inventoryClient;
    private final Map<String, Order> orderStore = new HashMap<>();
    private final Random random = new Random();

    public OrderService(InventoryClient inventoryClient) {
        this.inventoryClient = inventoryClient;
        orderStore.put("ORD-001", new Order("ORD-001", "CUST-A", "PROD-100", 2, new BigDecimal("49.98"), "COMPLETED"));
        orderStore.put("ORD-002", new Order("ORD-002", "CUST-B", "PROD-200", 1, new BigDecimal("29.99"), "PENDING"));
        orderStore.put("ORD-003", new Order("ORD-003", "CUST-A", "PROD-300", 5, new BigDecimal("124.95"), "COMPLETED"));
        orderStore.put("ORD-ERR", new Order("ORD-ERR", "CUST-X", "PROD-999", 1, new BigDecimal("0.00"), "ERROR"));
    }

    public Collection<Order> listOrders() {
        if (random.nextInt(10) < 2) {
            try {
                Thread.sleep(2000 + random.nextInt(2000));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        return orderStore.values();
    }

    public Order getOrder(String orderId) {
        try {
            Order order = orderStore.get(orderId);
            if (order != null && order.getStatus().equals("ERROR")) {
                String boom = null;
                boom.length(); // NullPointerException
            }
            return order;
        } catch (NullPointerException e) {
            return null;
        }
    }

    public Order createOrder(String customerId, String productId, int quantity, BigDecimal unitPrice) {
        log.info("Creating order for customer {} product {} qty {}", customerId, productId, quantity);

        InventoryResponse inventory = inventoryClient.checkInventory(productId);

        String orderId = "ORD-" + System.currentTimeMillis();
        BigDecimal total = unitPrice.multiply(BigDecimal.valueOf(quantity));
        String status = inventory.isAvailable() ? "CONFIRMED" : "REJECTED";

        Order order = new Order(orderId, customerId, productId, quantity, total, status);
        orderStore.put(orderId, order);

        log.info("Order {} created with status {}", orderId, status);
        return order;
    }
}
