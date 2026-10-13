package com.bny.spark26.lab.controller;

import com.bny.spark26.lab.model.Order;
import com.bny.spark26.lab.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private static final Logger log = LoggerFactory.getLogger(OrderController.class);
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public ResponseEntity<Collection<Order>> listOrders() {
        log.info("GET /orders");
        return ResponseEntity.ok(orderService.listOrders());
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<Order> getOrder(@PathVariable String orderId) {
        log.info("GET /orders/{}", orderId);
        return ResponseEntity.ok(orderService.getOrder(orderId));
    }

    @PostMapping
    public ResponseEntity<Order> createOrder(@RequestBody Map<String, Object> body) {
        String customerId = (String) body.get("customerId");
        String productId  = (String) body.get("productId");
        int quantity      = (int) body.get("quantity");
        BigDecimal price  = new BigDecimal(body.get("unitPrice").toString());

        Order order = orderService.createOrder(customerId, productId, quantity, price);
        return ResponseEntity.ok(order);
    }
}
