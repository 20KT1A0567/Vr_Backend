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
        String msg = userMessage == null ? "" : userMessage.toLowerCase();

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
        List<Integer> ramOptions = null;
        java.util.regex.Pattern ramPattern = java.util.regex.Pattern.compile("(\\d+)\\s*(?:gb)?\\s*ram", java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher ramMatcher = ramPattern.matcher(msg);
        while (ramMatcher.find()) {
            try {
                int r = Integer.parseInt(ramMatcher.group(1));
                if (r == 4 || r == 8 || r == 16 || r == 32 || r == 64) {
                    if (ramOptions == null) ramOptions = new ArrayList<>();
                    ramOptions.add(r);
                }
            } catch (NumberFormatException ignored) {}
        }
        // Also catch "16gb" without "ram"
        if (ramOptions == null) {
            java.util.regex.Matcher gbMatcher = java.util.regex.Pattern.compile("(4|8|16|32|64)\\s*gb", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(msg);
            while (gbMatcher.find()) {
                if (ramOptions == null) ramOptions = new ArrayList<>();
                ramOptions.add(Integer.parseInt(gbMatcher.group(1)));
            }
        }

        // 3. Brand / keyword detection
        String query = null;
        if (msg.contains("dell")) query = "Dell";
        else if (msg.contains("hp") || msg.contains("hewlett")) query = "HP";
        else if (msg.contains("lenovo") || msg.contains("thinkpad") || msg.contains("ideapad")) query = "Lenovo";
        else if (msg.contains("macbook") || msg.contains("apple") || msg.contains("mac")) query = "Apple";
        else if (msg.contains("asus") || msg.contains("rog") || msg.contains("vivobook") || msg.contains("zenbook")) query = "Asus";
        else if (msg.contains("acer") || msg.contains("aspire") || msg.contains("nitro")) query = "Acer";
        else if (msg.contains("msi")) query = "MSI";
        else if (msg.contains("samsung")) query = "Samsung";
        else if (msg.contains("gaming")) query = "gaming";
        else if (msg.contains("student") || msg.contains("study") || msg.contains("college")) query = "student";
        else if (msg.contains("office") || msg.contains("work") || msg.contains("business")) query = "office";
        else if (msg.contains("video edit") || msg.contains("editing")) query = "editing";
        else if (msg.contains("design") || msg.contains("graphic")) query = "design";

        // 4. Query DB
        final java.math.BigDecimal finalMaxPrice = maxPrice;
        final List<Integer> finalRamOptions = ramOptions;
        final String finalQuery = query;
        List<Product> allAvailable = productRepository.findByAvailableTrueOrderByUpdatedAtDesc();
        List<Product> filtered = allAvailable.stream()
            .filter(p -> {
                if (finalRamOptions != null && !finalRamOptions.isEmpty()) {
                    if (p.getRamGb() == null || !finalRamOptions.contains(p.getRamGb())) return false;
                }
                if (finalMaxPrice != null && p.getPrice() != null) {
                    if (p.getPrice().compareTo(finalMaxPrice) > 0) return false;
                }
                if (finalQuery != null) {
                    String title = p.getTitle() != null ? p.getTitle().toLowerCase() : "";
                    String brand = (p.getBrand() != null && p.getBrand().getName() != null) ? p.getBrand().getName().toLowerCase() : "";
                    String proc = p.getProcessor() != null ? p.getProcessor().toLowerCase() : "";
                    if (!title.contains(finalQuery.toLowerCase()) && !brand.contains(finalQuery.toLowerCase()) && !proc.contains(finalQuery.toLowerCase())) {
                        return false;
                    }
                }
                return true;
            })
            .limit(4)
            .collect(Collectors.toList());

        // 5. If no matches, return top available products
        if (filtered.isEmpty()) {
            filtered = allAvailable.stream().limit(4).collect(Collectors.toList());
        }

        // 6. Build human-readable reply
        StringBuilder reply = new StringBuilder("Great choice! Here are");
        if (query != null) reply.append(" ").append(query);
        if (finalRamOptions != null && !finalRamOptions.isEmpty()) reply.append(" ").append(finalRamOptions.get(0)).append("GB RAM");
        reply.append(" laptops");
        if (finalMaxPrice != null) reply.append(" under ₹").append(String.format("%,.0f", finalMaxPrice));
        reply.append(" from our certified refurbished inventory:");

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
