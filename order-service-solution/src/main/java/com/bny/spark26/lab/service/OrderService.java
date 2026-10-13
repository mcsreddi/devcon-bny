package com.bny.spark26.lab.service;

import com.bny.spark26.lab.client.InventoryClient;
import com.bny.spark26.lab.model.InventoryResponse;
import com.bny.spark26.lab.model.Order;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.instrumentation.annotations.WithSpan;
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

    /**
     * SOLUTION for BUG #2 — Latency spike now visible and annotated in traces.
     *
     * @WithSpan creates a child span for this method — the slow path adds a
     * span event "slow_path_triggered" with the delay amount. In Jaeger you can
     * now filter by duration and see exactly which listOrders() call was slow
     * and WHY (the span event tells you).
     */
    @WithSpan("OrderService.listOrders")
    public Collection<Order> listOrders() {
        Span span = Span.current();
        if (random.nextInt(10) < 2) {
            try {
                long delay = 2000 + random.nextInt(2000);
                // SOLUTION: record the slow path as a span event — visible in Jaeger
                span.addEvent("slow_path_triggered");
                span.setAttribute("slow_path.delay_ms", delay);
                log.warn("Slow path triggered for listOrders, delay={}ms", delay);
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                span.recordException(e);
            }
        }
        span.setAttribute("orders.count", orderStore.size());
        return orderStore.values();
    }

    /**
     * SOLUTION for BUG #1 — Silent exception now surfaces as a trace error.
     *
     * Key changes:
     * 1. span.recordException(e) — the NPE is now in the trace
     * 2. span.setStatus(ERROR) — the span shows as red in Jaeger
     * 3. Rethrow as RuntimeException — the controller can now return 500 properly
     */
    @WithSpan("OrderService.getOrder")
    public Order getOrder(String orderId) {
        Span span = Span.current();
        span.setAttribute("order.id", orderId);

        try {
            Order order = orderStore.get(orderId);
            if (order != null && order.getStatus().equals("ERROR")) {
                String boom = null;
                boom.length(); // NullPointerException
            }
            return order;
        } catch (NullPointerException e) {
            // SOLUTION: record on span instead of swallowing
            span.recordException(e);
            span.setStatus(StatusCode.ERROR, "Order processing failed for: " + orderId);
            log.error("Order processing failed for orderId={}", orderId, e);
            // Rethrow so the controller can return the right HTTP status
            throw new RuntimeException("Order processing error for: " + orderId, e);
        }
    }

    @WithSpan("OrderService.createOrder")
    public Order createOrder(String customerId, String productId, int quantity, BigDecimal unitPrice) {
        Span span = Span.current();
        span.setAttribute("order.customer_id", customerId);
        span.setAttribute("order.product_id", productId);
        span.setAttribute("order.quantity", quantity);

        log.info("Creating order for customer {} product {} qty {}", customerId, productId, quantity);

        InventoryResponse inventory = inventoryClient.checkInventory(productId);
        log.info("Inventory check result: available={} stock={}", inventory.isAvailable(), inventory.getAvailableStock());

        String orderId = "ORD-" + System.currentTimeMillis();
        BigDecimal total = unitPrice.multiply(BigDecimal.valueOf(quantity));
        String status = inventory.isAvailable() ? "CONFIRMED" : "REJECTED";

        Order order = new Order(orderId, customerId, productId, quantity, total, status);
        orderStore.put(orderId, order);

        span.setAttribute("order.id", orderId);
        span.setAttribute("order.status", status);
        log.info("Order {} created with status {}", orderId, status);
        return order;
    }
}
