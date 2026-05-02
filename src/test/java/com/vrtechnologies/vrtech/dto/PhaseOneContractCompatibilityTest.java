package com.vrtechnologies.vrtech.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vrtechnologies.vrtech.dto.request.BannerRequest;
import com.vrtechnologies.vrtech.dto.request.ProductRequest;
import com.vrtechnologies.vrtech.dto.request.ProductSectionRequest;
import com.vrtechnologies.vrtech.dto.response.BannerResponse;
import com.vrtechnologies.vrtech.dto.response.ProductResponse;
import com.vrtechnologies.vrtech.entity.enums.ProductSectionType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PhaseOneContractCompatibilityTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void productRequestAcceptsPhaseOneAliases() throws Exception {
        ProductRequest request = objectMapper.readValue("""
                {
                  "title": "Refurb Pro 14",
                  "brandId": 1,
                  "categoryId": 2,
                  "storeIds": [3],
                  "price": 64999,
                  "discountPercentage": 18,
                  "isFeatured": true,
                  "isBestSeller": true,
                  "isTodayDeal": true,
                  "videoUrl": "https://cdn.example.com/products/refurb-pro-14.mp4"
                }
                """, ProductRequest.class);

        assertThat(request.getDiscountPercent()).isEqualTo(18);
        assertThat(request.getFeatured()).isTrue();
        assertThat(request.getBestSeller()).isTrue();
        assertThat(request.getTodayDeal()).isTrue();
        assertThat(request.getVideoUrl()).isEqualTo("https://cdn.example.com/products/refurb-pro-14.mp4");
    }

    @Test
    void bannerRequestAcceptsPhaseOneAliases() throws Exception {
        BannerRequest request = objectMapper.readValue("""
                {
                  "title": "Weekend Deal",
                  "desktopImage": "https://cdn.example.com/banners/weekend-desktop.jpg",
                  "mobileImage": "https://cdn.example.com/banners/weekend-mobile.jpg",
                  "videoUrl": "https://cdn.example.com/banners/weekend.mp4",
                  "ctaText": "Shop now",
                  "ctaLink": "/products?deal=weekend"
                }
                """, BannerRequest.class);

        assertThat(request.getDesktopImageUrl()).isEqualTo("https://cdn.example.com/banners/weekend-desktop.jpg");
        assertThat(request.getMobileImageUrl()).isEqualTo("https://cdn.example.com/banners/weekend-mobile.jpg");
        assertThat(request.getLinkUrl()).isEqualTo("/products?deal=weekend");
        assertThat(request.getVideoUrl()).isEqualTo("https://cdn.example.com/banners/weekend.mp4");
    }

    @Test
    void productSectionRequestAcceptsDateAliasesAndProductIds() throws Exception {
        ProductSectionRequest request = objectMapper.readValue("""
                {
                  "title": "Today's Deals",
                  "sectionType": "TODAYS_DEALS",
                  "startDate": "2026-05-01T09:00:00",
                  "endDate": "2026-05-03T23:59:00",
                  "productIds": [101, 102, 103]
                }
                """, ProductSectionRequest.class);

        assertThat(request.getSectionType()).isEqualTo(ProductSectionType.TODAYS_DEALS);
        assertThat(request.getStartAt()).isEqualTo(LocalDateTime.of(2026, 5, 1, 9, 0));
        assertThat(request.getEndAt()).isEqualTo(LocalDateTime.of(2026, 5, 3, 23, 59));
        assertThat(request.getProductIds()).containsExactly(101L, 102L, 103L);
    }

    @Test
    void responsesExposePhaseOneAliasFields() {
        ProductResponse productResponse = ProductResponse.builder()
                .id(11L)
                .title("Refurb Pro 14")
                .price(BigDecimal.valueOf(64999))
                .discountPercent(18)
                .featured(true)
                .bestSeller(true)
                .todayDeal(true)
                .build();
        BannerResponse bannerResponse = BannerResponse.builder()
                .id(21L)
                .title("Weekend Deal")
                .desktopImageUrl("https://cdn.example.com/banners/weekend-desktop.jpg")
                .mobileImageUrl("https://cdn.example.com/banners/weekend-mobile.jpg")
                .linkUrl("/products?deal=weekend")
                .build();

        JsonNode productJson = objectMapper.valueToTree(productResponse);
        JsonNode bannerJson = objectMapper.valueToTree(bannerResponse);

        assertThat(productJson.path("discountPercent").asInt()).isEqualTo(18);
        assertThat(productJson.path("discountPercentage").asInt()).isEqualTo(18);
        assertThat(productJson.path("featured").asBoolean()).isTrue();
        assertThat(productJson.path("isFeatured").asBoolean()).isTrue();
        assertThat(productJson.path("bestSeller").asBoolean()).isTrue();
        assertThat(productJson.path("isBestSeller").asBoolean()).isTrue();
        assertThat(productJson.path("todayDeal").asBoolean()).isTrue();
        assertThat(productJson.path("isTodayDeal").asBoolean()).isTrue();

        assertThat(bannerJson.path("desktopImageUrl").asText()).isEqualTo("https://cdn.example.com/banners/weekend-desktop.jpg");
        assertThat(bannerJson.path("desktopImage").asText()).isEqualTo("https://cdn.example.com/banners/weekend-desktop.jpg");
        assertThat(bannerJson.path("mobileImageUrl").asText()).isEqualTo("https://cdn.example.com/banners/weekend-mobile.jpg");
        assertThat(bannerJson.path("mobileImage").asText()).isEqualTo("https://cdn.example.com/banners/weekend-mobile.jpg");
        assertThat(bannerJson.path("linkUrl").asText()).isEqualTo("/products?deal=weekend");
        assertThat(bannerJson.path("ctaLink").asText()).isEqualTo("/products?deal=weekend");
    }
}
