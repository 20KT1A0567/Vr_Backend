package com.vrtechnologies.vrtech.controller;

import com.vrtechnologies.vrtech.dto.event.SystemEvent;
import com.vrtechnologies.vrtech.dto.response.ProductResponse;
import com.vrtechnologies.vrtech.service.ProductService;
import com.vrtechnologies.vrtech.service.SseEmitterService;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/public/support")
public class SupportController {
    private final SseEmitterService sseEmitterService;
    private final ProductService productService;

    public SupportController(SseEmitterService sseEmitterService, ProductService productService) {
        this.sseEmitterService = sseEmitterService;
        this.productService = productService;
    }

    @PostMapping("/assist")
    public Map<String, Object> requestAssist(@RequestBody Map<String, Object> payload) {
        SystemEvent assistEvent = SystemEvent.builder()
                .eventType("SUPPORT_ASSIST")
                .title("Live Assist Requested")
                .message("Customer \"" + payload.getOrDefault("customerName", "Guest") + "\" is encountering issues at checkout.")
                .severity("INFO")
                .payload(payload)
                .timestamp(LocalDateTime.now())
                .build();
        sseEmitterService.broadcast(assistEvent);
        return Map.of("success", true, "message", "Support request broadcasted successfully");
    }

    @PostMapping("/chat")
    public Map<String, Object> chat(@RequestBody Map<String, String> payload) {
        String userMessage = payload.getOrDefault("message", "");
        String msgLower = userMessage.toLowerCase();

        // 1. Parse budget / maxPrice
        BigDecimal maxPrice = null;
        // Match patterns like "under 50000", "below 35k", "under 40,000"
        Pattern pricePattern = Pattern.compile("(?:under|below|less than|within|budget of)?\\s*(?:rs\\.?|inr|₹)?\\s*(\\d+)\\s*(k)?");
        Matcher priceMatcher = pricePattern.matcher(msgLower);
        while (priceMatcher.find()) {
            String valueStr = priceMatcher.group(1);
            String kSuffix = priceMatcher.group(2);
            try {
                long val = Long.parseLong(valueStr);
                if (kSuffix != null && !kSuffix.isEmpty()) {
                    val *= 1000;
                }
                if (val > 1000) {
                    maxPrice = BigDecimal.valueOf(val);
                }
            } catch (NumberFormatException ignored) {}
        }

        if (maxPrice == null) {
            Pattern kPattern = Pattern.compile("(\\d+)\\s*k\\b");
            Matcher kMatcher = kPattern.matcher(msgLower);
            if (kMatcher.find()) {
                try {
                    maxPrice = BigDecimal.valueOf(Long.parseLong(kMatcher.group(1)) * 1000);
                } catch (NumberFormatException ignored) {}
            }
        }

        // 2. Parse RAM constraints (e.g. 8gb, 16gb, 32gb)
        List<Integer> ramOptions = null;
        Pattern ramPattern = Pattern.compile("(\\d+)\\s*(?:gb|g)?\\s*(?:ram)?");
        Matcher ramMatcher = ramPattern.matcher(msgLower);
        while (ramMatcher.find()) {
            try {
                int ramVal = Integer.parseInt(ramMatcher.group(1));
                if (ramVal == 4 || ramVal == 8 || ramVal == 12 || ramVal == 16 || ramVal == 24 || ramVal == 32 || ramVal == 64) {
                    if (ramOptions == null) {
                        ramOptions = new ArrayList<>();
                    }
                    ramOptions.add(ramVal);
                }
            } catch (NumberFormatException ignored) {}
        }

        // 3. Brand / search text query
        String query = null;
        if (msgLower.contains("dell")) {
            query = "Dell";
        } else if (msgLower.contains("hp")) {
            query = "HP";
        } else if (msgLower.contains("lenovo") || msgLower.contains("thinkpad")) {
            query = "Lenovo";
        } else if (msgLower.contains("macbook") || msgLower.contains("apple") || msgLower.contains("mac")) {
            query = "Apple";
        } else if (msgLower.contains("asus") || msgLower.contains("rog")) {
            query = "Asus";
        } else if (msgLower.contains("acer")) {
            query = "Acer";
        } else if (msgLower.contains("gaming")) {
            query = "gaming";
        } else if (msgLower.contains("student") || msgLower.contains("office")) {
            query = "student";
        } else {
            // General word search fallback (e.g., compile a query from words with length > 3)
            String[] words = msgLower.replaceAll("[^a-zA-Z0-9\\s]", "").split("\\s+");
            for (String w : words) {
                if (w.length() > 3 && !w.equals("laptop") && !w.equals("laptops") && !w.equals("show") && !w.equals("find") && !w.equals("need") && !w.equals("want")) {
                    query = w;
                    break;
                }
            }
        }

        // Call the catalog database to fetch active and in-stock items matching these requirements
        List<ProductResponse> matches = productService.getPublicProducts(
                query,
                null, // brandIds
                null, // categoryIds
                null, // storeId
                ramOptions,
                null, // storageOptions
                null, // processorOptions
                null, // osOptions
                null, // displaySizeOptions
                null, // displayTypeOptions
                null, // graphicsOptions
                null, // conditions
                true, // inStockOnly
                null, // featured
                null, // bestSeller
                null, // todayDeal
                null, // minPrice
                maxPrice
        );

        List<ProductResponse> limitedMatches = matches.stream().limit(4).toList();

        // If no matches, fallback to general featured laptops so the user is never left with an empty list
        String replyText;
        if (limitedMatches.isEmpty()) {
            List<ProductResponse> fallbacks = productService.getFeaturedProducts(3);
            replyText = "I couldn't find any laptops in our inventory matching those exact specifications, but here are some of our top trending, fully certified units in stock today:";
            Map<String, Object> result = new HashMap<>();
            result.put("replyText", replyText);
            result.put("products", fallbacks);
            return result;
        }

        // Build descriptive response based on parsed fields
        StringBuilder sb = new StringBuilder();
        sb.append("I've searched our real-time catalog and found some great matches");
        if (query != null) {
            sb.append(" for ").append(query);
        }
        if (maxPrice != null) {
            sb.append(" under ₹").append(String.format("%,d", maxPrice.longValue()));
        }
        if (ramOptions != null && !ramOptions.isEmpty()) {
            sb.append(" with ").append(ramOptions.get(0)).append("GB RAM");
        }
        sb.append(":");
        replyText = sb.toString();

        Map<String, Object> result = new HashMap<>();
        result.put("replyText", replyText);
        result.put("products", limitedMatches);
        return result;
    }
}
