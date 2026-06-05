package com.vrtechnologies.vrtech.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vrtechnologies.vrtech.dto.SupportChatRequest;
import com.vrtechnologies.vrtech.dto.SupportChatResponse;
import com.vrtechnologies.vrtech.entity.Product;
import com.vrtechnologies.vrtech.repository.ProductRepository;
import com.vrtechnologies.vrtech.dto.response.ProductResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SupportChatService {

    private final ProductRepository productRepository;
    private final ProductService productService;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${GEMINI_API_KEY:}")
    private String geminiApiKey;

    private static final String GEMINI_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=";

    public SupportChatResponse handleChat(SupportChatRequest request) {
        log.info("Handling support chat for message: {}", request.getMessage());

        if (geminiApiKey == null || geminiApiKey.trim().isEmpty()) {
            log.info("GEMINI_API_KEY not configured — using local smart matching.");
            return localSmartMatch(request.getMessage());
        }

        // Fetch top 150 available products for context
        List<Product> availableProducts = productRepository.findByAvailableTrueOrderByUpdatedAtDesc();
        List<Product> contextProducts = availableProducts.stream().limit(150).collect(Collectors.toList());

        String productCatalogContext = buildCatalogJson(contextProducts);

        String systemInstruction = "You are the AI Concierge for VR Technologies, a refurbished laptop marketplace. " +
                "You help customers find the best laptops for their needs.\n" +
                "Here is our current active product catalog in JSON format:\n" +
                productCatalogContext + "\n\n" +
                "Instructions:\n" +
                "1. Answer the user's question politely and concisely (under 2 sentences).\n" +
                "2. Analyze their request (e.g. budget, use-case, specs) and pick up to 4 best matching product IDs from the catalog.\n" +
                "3. You MUST respond with ONLY a valid JSON object in this exact format:\n" +
                "{\n" +
                "  \"replyText\": \"Your polite conversational answer here\",\n" +
                "  \"productIds\": [1, 2, 3]\n" +
                "}\n" +
                "Do not include markdown blocks or any other text outside the JSON object.";

        try {
            List<Map<String, Object>> contents = new ArrayList<>();
            if (request.getHistory() != null && !request.getHistory().isEmpty()) {
                for (SupportChatRequest.ChatMessage msg : request.getHistory()) {
                    if (msg.getContent() == null || msg.getContent().trim().isEmpty()) continue;
                    String role = "user".equalsIgnoreCase(msg.getRole()) ? "user" : "model";
                    contents.add(Map.of(
                        "role", role,
                        "parts", List.of(Map.of("text", msg.getContent()))
                    ));
                }
            } else {
                contents.add(Map.of(
                    "role", "user",
                    "parts", List.of(Map.of("text", request.getMessage()))
                ));
            }

            Map<String, Object> systemInstructionObj = Map.of(
                "parts", List.of(Map.of("text", systemInstruction))
            );

            Map<String, Object> generationConfig = Map.of("temperature", 0.1, "maxOutputTokens", 512);
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("contents", contents);
            requestBody.put("systemInstruction", systemInstructionObj);
            requestBody.put("generationConfig", generationConfig);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            String responseJson = restTemplate.postForObject(GEMINI_URL + geminiApiKey, entity, String.class);

            return parseGeminiResponse(responseJson, contextProducts);

        } catch (Exception e) {
            log.error("Error communicating with Gemini API: {}", e.getMessage(), e);
            return localSmartMatch(request.getMessage());
        }
    }

    private SupportChatResponse parseGeminiResponse(String responseJson, List<Product> contextProducts) {
        try {
            JsonNode root = objectMapper.readTree(responseJson);
            // Gemini response: candidates[0].content.parts[0].text
            String content = root.path("candidates").get(0)
                    .path("content").path("parts").get(0)
                    .path("text").asText();

            // Strip markdown fences if present
            content = content.replaceAll("```json", "").replaceAll("```", "").trim();
            // Extract first JSON object
            int start = content.indexOf('{');
            int end = content.lastIndexOf('}');
            if (start >= 0 && end > start) content = content.substring(start, end + 1);

            JsonNode contentJson = objectMapper.readTree(content);
            String replyText = contentJson.path("replyText").asText("Here are some recommendations:");

            List<Long> productIds = new ArrayList<>();
            JsonNode idsNode = contentJson.path("productIds");
            if (idsNode.isArray()) {
                for (JsonNode idNode : idsNode) productIds.add(idNode.asLong());
            }

            List<Product> matchedProducts = productRepository.findByIdIn(productIds);
            Map<Long, Product> productMap = matchedProducts.stream().collect(Collectors.toMap(Product::getId, p -> p));
            List<Product> orderedProducts = productIds.stream()
                    .map(productMap::get)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            return SupportChatResponse.builder()
                    .replyText(replyText)
                    .products(productService.toProductResponseList(orderedProducts))
                    .build();

        } catch (Exception e) {
            log.error("Failed to parse Gemini response: {}", e.getMessage(), e);
            return localSmartMatch("");
        }
    }

    /**
     * Local smart matcher — works without OpenAI.
     * Parses budget, RAM, and brand keywords from the message and queries the DB.
     */
    private SupportChatResponse localSmartMatch(String userMessage) {
        String msg = userMessage == null ? "" : userMessage.toLowerCase().trim();

        // 1. Parse budget / maxPrice
        java.math.BigDecimal maxPrice = null;
        java.util.regex.Pattern pricePattern = java.util.regex.Pattern.compile(
                "(?:under|below|less than|within|budget|max|upto)?\\s*(?:rs\\.?|inr|₹)?\\s*(\\d[\\d,]*)\\s*(k)?",
                java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher priceMatcher = pricePattern.matcher(msg);
        while (priceMatcher.find()) {
            try {
                String raw = priceMatcher.group(1).replace(",", "");
                long val = Long.parseLong(raw);
                if (priceMatcher.group(2) != null) val *= 1000;
                if (val >= 5000 && val <= 500000) {
                    maxPrice = java.math.BigDecimal.valueOf(val);
                }
            } catch (NumberFormatException ignored) {}
        }

        // 2. Parse RAM requirements
        List<Integer> ramOptions = new ArrayList<>();
        java.util.regex.Pattern ramPattern = java.util.regex.Pattern.compile("(\\d+)\\s*(?:gb)?\\s*ram", java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher ramMatcher = ramPattern.matcher(msg);
        while (ramMatcher.find()) {
            try {
                int r = Integer.parseInt(ramMatcher.group(1));
                if (r == 4 || r == 8 || r == 16 || r == 32 || r == 64) {
                    ramOptions.add(r);
                }
            } catch (NumberFormatException ignored) {}
        }
        if (ramOptions.isEmpty()) {
            java.util.regex.Matcher gbMatcher = java.util.regex.Pattern.compile("(4|8|16|32|64)\\s*gb", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(msg);
            while (gbMatcher.find()) {
                ramOptions.add(Integer.parseInt(gbMatcher.group(1)));
            }
        }

        // 3. Extract search terms (tokens)
        Set<String> stopwords = Set.of(
            "a", "an", "the", "in", "on", "at", "for", "to", "of", "and", "or", "with", "under", "below", "above",
            "laptop", "laptops", "notebook", "refurbished", "show", "me", "find", "search", "get", "recommend",
            "want", "need", "i", "buy", "price", "budget", "rs", "rupees", "inr", "gb", "ram", "ssd", "hdd",
            "processor", "please", "can", "you", "thanks", "hello", "hi", "hey", "looking"
        );

        String cleanMsg = msg.replaceAll("[^a-zA-Z0-9\\s]", " ");
        String[] tokens = cleanMsg.split("\\s+");
        List<String> searchTerms = new ArrayList<>();
        for (String token : tokens) {
            if (!token.isEmpty() && !stopwords.contains(token)) {
                searchTerms.add(token);
            }
        }

        // 4. Retrieve and score products
        List<Product> allAvailable = productRepository.findByAvailableTrueOrderByUpdatedAtDesc();
        
        class ScoredProduct {
            Product product;
            int score;
            ScoredProduct(Product p, int s) { this.product = p; this.score = s; }
        }

        List<ScoredProduct> scoredList = new ArrayList<>();
        final java.math.BigDecimal finalMaxPrice = maxPrice;
        final List<Integer> finalRamOptions = ramOptions;

        // Use-case and OS indicators
        boolean queryMentionsMac = msg.contains("mac") || msg.contains("apple") || msg.contains("macos") || msg.contains("ios");
        boolean queryMentionsWindows = msg.contains("windows") || msg.contains("win10") || msg.contains("win11") || msg.contains("win 10") || msg.contains("win 11");
        boolean queryMentionsGaming = msg.contains("game") || msg.contains("gaming") || msg.contains("gpu") || msg.contains("graphics") || msg.contains("rtx") || msg.contains("gtx");
        boolean queryMentionsCoding = msg.contains("code") || msg.contains("coding") || msg.contains("program") || msg.contains("developer") || msg.contains("dev") || msg.contains("programming");

        for (Product p : allAvailable) {
            // Hard filters
            if (finalMaxPrice != null && p.getPrice() != null && p.getPrice().compareTo(finalMaxPrice) > 0) {
                continue;
            }
            if (!finalRamOptions.isEmpty() && p.getRamGb() != null && !finalRamOptions.contains(p.getRamGb())) {
                continue;
            }

            int score = 0;
            String title = p.getTitle() == null ? "" : p.getTitle().toLowerCase();
            String brand = (p.getBrand() != null && p.getBrand().getName() != null) ? p.getBrand().getName().toLowerCase() : "";
            String category = (p.getCategory() != null && p.getCategory().getName() != null) ? p.getCategory().getName().toLowerCase() : "";
            String proc = p.getProcessor() == null ? "" : p.getProcessor().toLowerCase();
            String desc = p.getDescription() == null ? "" : p.getDescription().toLowerCase();

            // 1. Text term matching
            for (String term : searchTerms) {
                if (brand.contains(term)) score += 10;
                if (title.contains(term)) score += 5;
                if (category.contains(term)) score += 4;
                if (proc.contains(term)) score += 3;
                if (desc.contains(term)) score += 1;
                
                // Match storage terms (e.g. "512", "256")
                if (p.getStorageGb() != null && term.equals(p.getStorageGb().toString())) {
                    score += 5;
                }
            }

            // 2. OS / Platform Boosts
            if (queryMentionsMac) {
                if (brand.contains("apple") || category.contains("macbook") || title.contains("macbook") || title.contains("apple")) {
                    score += 100; // Force-rank matching Macs to the top
                } else {
                    score -= 50;  // Demote other systems
                }
            }
            if (queryMentionsWindows) {
                if (!brand.contains("apple") && !category.contains("macbook") && !title.contains("macbook")) {
                    score += 50;  // Boost non-Macs
                } else {
                    score -= 50;  // Demote Macs
                }
            }

            // 3. Gaming Boost
            if (queryMentionsGaming) {
                if (category.contains("gaming") || title.contains("gaming") || desc.contains("gaming") ||
                    (p.getGraphicsCard() != null && !p.getGraphicsCard().trim().isEmpty() && 
                     !p.getGraphicsCard().toLowerCase().contains("intel iris") && 
                     !p.getGraphicsCard().toLowerCase().contains("integrated"))) {
                    score += 40;
                }
            }

            // 4. Developer / RAM Boost
            if (queryMentionsCoding) {
                // Developers love high RAM (16GB or 32GB)
                if (p.getRamGb() != null && p.getRamGb() >= 16) {
                    score += 30;
                }
                if (desc.contains("coding") || desc.contains("programming") || desc.contains("developer")) {
                    score += 20;
                }
            }
            
            // Give a tiny boost for newer/featured products to break ties
            if (p.isFeatured()) score += 1;
            
            scoredList.add(new ScoredProduct(p, score));
        }

        // Sort by score descending
        scoredList.sort((a, b) -> Integer.compare(b.score, a.score));

        // Get top 4 products
        List<Product> filtered = scoredList.stream()
                .map(sp -> sp.product)
                .limit(4)
                .collect(Collectors.toList());

        // If no matches found and we had search terms, fallback to top available products
        boolean noMatchesUnderPrice = false;
        if (filtered.isEmpty()) {
            if (maxPrice != null) {
                // Fetch available products sorted by price ascending to show the cheapest options
                filtered = allAvailable.stream()
                        .sorted(Comparator.comparing(Product::getPrice))
                        .limit(4)
                        .collect(Collectors.toList());
                noMatchesUnderPrice = true;
            } else {
                filtered = allAvailable.stream().limit(4).collect(Collectors.toList());
            }
        }

        // 5. Build human-readable reply
        StringBuilder reply = new StringBuilder();
        if (noMatchesUnderPrice) {
            java.math.BigDecimal startPrice = filtered.isEmpty() ? java.math.BigDecimal.ZERO : filtered.get(0).getPrice();
            reply.append("We couldn't find any refurbished laptops under ₹")
                 .append(String.format("%,.0f", maxPrice))
                 .append(" in our current inventory. Here are our most affordable options starting at ₹")
                 .append(String.format("%,.0f", startPrice))
                 .append(":");
        } else if (maxPrice != null) {
            reply.append("Here are laptops under ₹").append(String.format("%,.0f", maxPrice)).append(":");
        } else {
            reply.append("Here are the best refurbished laptops matching your search:");
        }

        return SupportChatResponse.builder()
                .replyText(reply.toString())
                .products(productService.toProductResponseList(filtered))
                .build();
    }

    private String buildCatalogJson(List<Product> products) {
        List<Map<String, Object>> minimalProducts = products.stream().map(p -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", p.getId());
            map.put("title", p.getTitle());
            map.put("brand", p.getBrand() != null ? p.getBrand().getName() : null);
            map.put("category", p.getCategory() != null ? p.getCategory().getName() : null);
            map.put("price", p.getPrice());
            map.put("discountPercent", p.getDiscountPercent());
            map.put("processor", p.getProcessor());
            map.put("processorGen", p.getProcessorGeneration());
            map.put("ramGb", p.getRamGb());
            map.put("storageGb", p.getStorageGb());
            map.put("storageType", p.getStorageType());
            map.put("condition", p.getProductCondition() != null ? p.getProductCondition().name() : null);
            map.put("graphicsCard", p.getGraphicsCard());
            return map;
        }).collect(Collectors.toList());

        try {
            return objectMapper.writeValueAsString(minimalProducts);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    private SupportChatResponse parseOpenAiResponse(String responseJson, List<Product> contextProducts) {
        try {
            JsonNode root = objectMapper.readTree(responseJson);
            String content = root.path("choices").get(0).path("message").path("content").asText();
            
            // Clean markdown if the LLM ignored instructions
            content = content.replace("```json", "").replace("```", "").trim();
            
            JsonNode contentJson = objectMapper.readTree(content);
            String replyText = contentJson.path("replyText").asText("Here are some recommendations:");
            
            List<Long> productIds = new ArrayList<>();
            JsonNode idsNode = contentJson.path("productIds");
            if (idsNode.isArray()) {
                for (JsonNode idNode : idsNode) {
                    productIds.add(idNode.asLong());
                }
            }
            
            List<Product> matchedProducts = productRepository.findByIdIn(productIds);
            
            // Sort to maintain LLM's requested order
            Map<Long, Product> productMap = matchedProducts.stream().collect(Collectors.toMap(Product::getId, p -> p));
            List<Product> orderedProducts = productIds.stream()
                    .map(productMap::get)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
                    
            return SupportChatResponse.builder()
                    .replyText(replyText)
                    .products(productService.toProductResponseList(orderedProducts))
                    .build();
                    
        } catch (Exception e) {
            log.error("Failed to parse OpenAI response: {}", e.getMessage(), e);
            return fallbackResponse();
        }
    }

    private SupportChatResponse fallbackResponse() {
        return SupportChatResponse.builder()
                .replyText("I am currently experiencing technical difficulties connecting to my AI brain. Please try again later!")
                .products(new ArrayList<>())
                .build();
    }
}
