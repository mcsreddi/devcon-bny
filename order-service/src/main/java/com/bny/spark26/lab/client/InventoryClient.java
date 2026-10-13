package com.bny.spark26.lab.client;

import com.bny.spark26.lab.model.InventoryResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.ResourceAccessException;

@Component
public class InventoryClient {

    private final RestTemplate restTemplate;

    @Value("${inventory.service.url:http://localhost:8081}")
    private String inventoryServiceUrl;

    public InventoryClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public InventoryResponse checkInventory(String productId) {
        try {
            String url = inventoryServiceUrl + "/inventory/" + productId;
            return restTemplate.getForObject(url, InventoryResponse.class);
        } catch (ResourceAccessException e) {
            // Inventory is slow sometimes — just assume it's in stock
            return new InventoryResponse(productId, 999, true);
        } catch (Exception e) {
            return new InventoryResponse(productId, 0, false);
        }
    }
}
