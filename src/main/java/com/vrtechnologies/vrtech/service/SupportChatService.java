package com.vrtechnologies.vrtech.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vrtechnologies.vrtech.dto.SupportChatRequest;
import com.vrtechnologies.vrtech.dto.SupportChatResponse;
import com.vrtechnologies.vrtech.entity.Product;
import com.vrtechnologies.vrtech.repository.ProductRepository;
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
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${OPENAI_API_KEY:}")
    private String openAiApiKey;

    public SupportChatResponse handleChat(SupportChatRequest request) {
        log.info("Handling support chat for message: {}", request.getMessage());

        if (openAiApiKey == null || openAiApiKey.trim().isEmpty()) {
            log.warn("OPENAI_API_KEY is not configured! Falling back to basic response.");
            return fallbackResponse();
        }

        // Fetch top 50 available products for context
        List<Product> availableProducts = productRepository.findByAvailableTrueOrderByUpdatedAtDesc();
        List<Product> contextProducts = availableProducts.stream().limit(50).collect(Collectors.toList());

        String productCatalogContext = buildCatalogJson(contextProducts);

        String systemPrompt = "You are the AI Concierge for VR Technologies, a refurbished laptop marketplace. " +
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
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "gpt-4o-mini");
            
            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", "content", systemPrompt));
            messages.add(Map.of("role", "user", "content", request.getMessage()));
            requestBody.put("messages", messages);
            
            // Set temperature low for reliable JSON output
            requestBody.put("temperature", 0.1);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(openAiApiKey);
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            
            String responseJson = restTemplate.postForObject("https://api.openai.com/v1/chat/completions", entity, String.class);
            
            return parseOpenAiResponse(responseJson, contextProducts);
            
        } catch (Exception e) {
            log.error("Error communicating with OpenAI API: {}", e.getMessage(), e);
            return fallbackResponse();
        }
    }

    private String buildCatalogJson(List<Product> products) {
        List<Map<String, Object>> minimalProducts = products.stream().map(p -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", p.getId());
            map.put("title", p.getTitle());
            map.put("category", p.getCategory() != null ? p.getCategory().getName() : null);
            map.put("price", p.getPrice());
            map.put("discountPercent", p.getDiscountPercent());
            map.put("processor", p.getProcessor());
            map.put("ramGb", p.getRamGb());
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
                    .products(orderedProducts)
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
