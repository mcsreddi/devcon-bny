package com.bny.spark26.lab.client;

import com.bny.spark26.lab.model.InventoryResponse;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.ResourceAccessException;

@Component
public class InventoryClient {

    private static final Logger log = LoggerFactory.getLogger(InventoryClient.class);

    private final RestTemplate restTemplate;

    @Value("${inventory.service.url:http://localhost:8081}")
    private String inventoryServiceUrl;

    public InventoryClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * SOLUTION for BUG #3 — Downstream dependency failure now visible in traces.
     *
     * Key changes:
     * 1. span.recordException(e) — attaches full stack trace to the current trace span
     * 2. span.setStatus(StatusCode.ERROR, ...) — marks the span as failed in Jaeger UI
     * 3. Meaningful log message with exception — visible in log aggregation
     *
     * Now when inventory times out, the trace shows a red ERROR span and callers
     * can see exactly what happened and where.
     */
    public InventoryResponse checkInventory(String productId) {
        Span span = Span.current();
        span.setAttribute("inventory.product_id", productId);

        try {
            String url = inventoryServiceUrl + "/inventory/" + productId;
            InventoryResponse response = restTemplate.getForObject(url, InventoryResponse.class);
            span.setAttribute("inventory.available", response.isAvailable());
            span.setAttribute("inventory.stock", response.getAvailableStock());
            return response;
        } catch (ResourceAccessException e) {
            // SOLUTION: record the exception on the span — now visible in Jaeger
            span.recordException(e);
            span.setStatus(StatusCode.ERROR, "Inventory service timeout for product: " + productId);
            log.error("Inventory service timeout for product {}: {}", productId, e.getMessage());
            // Still returning fallback — but now callers KNOW it's a fallback
            span.setAttribute("inventory.fallback", true);
            return new InventoryResponse(productId, 999, true);
        } catch (Exception e) {
            span.recordException(e);
            span.setStatus(StatusCode.ERROR, "Inventory service error: " + e.getMessage());
            log.error("Inventory service error for product {}", productId, e);
            span.setAttribute("inventory.fallback", true);
            return new InventoryResponse(productId, 0, false);
        }
    }
}
