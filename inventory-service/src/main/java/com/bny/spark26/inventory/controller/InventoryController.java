package com.bny.spark26.inventory.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@RestController
@RequestMapping("/inventory")
public class InventoryController {

    private static final Logger log = LoggerFactory.getLogger(InventoryController.class);
    private final Random random = new Random();

    private final Map<String, Integer> stock = new HashMap<>() {{
        put("PROD-100", 50);
        put("PROD-200", 3);
        put("PROD-300", 0);  // out of stock
    }};

    /**
     * Simulates an unreliable downstream service.
     * 30% of requests for PROD-200 take 3+ seconds (timeout scenario).
     * Requests for unknown products always return 404.
     */
    @GetMapping("/{productId}")
    public Map<String, Object> checkInventory(@PathVariable String productId) throws InterruptedException {
        log.info("Inventory check for {}", productId);

        // Simulate intermittent slowness on PROD-200
        if ("PROD-200".equals(productId) && random.nextInt(10) < 3) {
            log.warn("PROD-200 slow path triggered");
            Thread.sleep(3500); // exceeds typical client timeout
        }

        Integer qty = stock.get(productId);
        if (qty == null) {
            throw new RuntimeException("Product not found: " + productId);
        }

        return Map.of(
            "productId", productId,
            "availableStock", qty,
            "available", qty > 0
        );
    }
}
