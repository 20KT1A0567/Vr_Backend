package com.vrtechnologies.vrtech.service;

import com.vrtechnologies.vrtech.config.RazorpayProperties;
import com.vrtechnologies.vrtech.exception.BadRequestException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class RazorpayService {

    private final RazorpayProperties properties;
    private final RestTemplate restTemplate = new RestTemplate();

    public RazorpayService(RazorpayProperties properties) {
        this.properties = properties;
    }

    public boolean isEnabled() {
        return properties.isEnabled()
                && properties.getKeyId() != null
                && !properties.getKeyId().isBlank()
                && properties.getKeySecret() != null
                && !properties.getKeySecret().isBlank();
    }

    public Map<String, Object> createOrder(BigDecimal amount, String receipt, Map<String, Object> notes) {
        if (!isEnabled()) {
            throw new BadRequestException("Online payments are not configured yet");
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("amount", toSubunits(amount));
        payload.put("currency", properties.getCurrency());
        payload.put("receipt", receipt);
        payload.put("notes", notes);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBasicAuth(properties.getKeyId(), properties.getKeySecret(), StandardCharsets.UTF_8);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    properties.getApiBaseUrl() + "/orders",
                    HttpMethod.POST,
                    new HttpEntity<>(payload, headers),
                    Map.class
            );
            Map<String, Object> body = response.getBody();
            if (body == null || body.get("id") == null) {
                throw new BadRequestException("Payment gateway did not return an order id");
            }
            return body;
        } catch (HttpStatusCodeException ex) {
            throw new BadRequestException("Failed to create Razorpay order: " + ex.getResponseBodyAsString());
        } catch (Exception ex) {
            throw new BadRequestException("Failed to connect to Razorpay");
        }
    }

    public void verifyPaymentSignature(String gatewayOrderId, String gatewayPaymentId, String signature) {
        String expected = hmacSha256(gatewayOrderId + "|" + gatewayPaymentId, properties.getKeySecret());
        if (!expected.equals(signature)) {
            throw new BadRequestException("Invalid Razorpay payment signature");
        }
    }

    public void verifyWebhookSignature(String payload, String signature) {
        if (properties.getWebhookSecret() == null || properties.getWebhookSecret().isBlank()) {
            throw new BadRequestException("Razorpay webhook secret is not configured");
        }
        String expected = hmacSha256(payload, properties.getWebhookSecret());
        if (!expected.equals(signature)) {
            throw new BadRequestException("Invalid Razorpay webhook signature");
        }
    }

    public String keyId() {
        return properties.getKeyId();
    }

    public String merchantName() {
        return properties.getMerchantName();
    }

    public String currency() {
        return properties.getCurrency();
    }

    private long toSubunits(BigDecimal amount) {
        return amount.multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .longValue();
    }

    private String hmacSha256(String message, String secret) {
        try {
            Mac sha256 = Mac.getInstance("HmacSHA256");
            sha256.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = sha256.doFinal(message.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(digest.length * 2);
            for (byte value : digest) {
                builder.append(String.format("%02x", value));
            }
            return builder.toString();
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to calculate HMAC", ex);
        }
    }
}
