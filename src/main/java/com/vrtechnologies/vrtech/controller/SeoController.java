package com.vrtechnologies.vrtech.controller;

import com.vrtechnologies.vrtech.entity.Category;
import com.vrtechnologies.vrtech.entity.CmsPage;
import com.vrtechnologies.vrtech.entity.Product;
import com.vrtechnologies.vrtech.entity.SeoSetting;
import com.vrtechnologies.vrtech.dto.response.ApiResponse;
import com.vrtechnologies.vrtech.dto.response.SeoSettingResponse;
import com.vrtechnologies.vrtech.exception.ResourceNotFoundException;
import com.vrtechnologies.vrtech.repository.CategoryRepository;
import com.vrtechnologies.vrtech.repository.CmsPageRepository;
import com.vrtechnologies.vrtech.repository.ProductRepository;
import com.vrtechnologies.vrtech.service.SeoSettingService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@RestController
public class SeoController {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final CmsPageRepository cmsPageRepository;
    private final SeoSettingService seoSettingService;

    @Value("${app.website.base-url:https://vr.anushatechnologies.com}")
    private String websiteBaseUrl;

    public SeoController(ProductRepository productRepository, CategoryRepository categoryRepository, CmsPageRepository cmsPageRepository, SeoSettingService seoSettingService) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.cmsPageRepository = cmsPageRepository;
        this.seoSettingService = seoSettingService;
    }

    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public String sitemap() {
        String base = websiteBaseUrl.replaceAll("/+$", "");
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");
        if (isSitemapEnabled("HOME", null, null)) {
            appendUrl(xml, base + "/", null, "daily", "1.0");
        }
        if (isSitemapEnabled("PRODUCT_LIST", null, null)) {
            appendUrl(xml, base + "/products", null, "daily", "0.9");
        }
        appendUrl(xml, base + "/stores", null, "weekly", "0.7");
        for (Category category : categoryRepository.findAll()) {
            if (isSitemapEnabled("CATEGORY", category.getId(), category.getSlug())) {
                appendUrl(xml, base + "/products?categoryId=" + category.getId(), null, "weekly", "0.7");
            }
        }
        for (Product product : productRepository.findByAvailableTrueOrderByUpdatedAtDesc()) {
            if (isSitemapEnabled("PRODUCT", product.getId(), null)) {
                String updated = product.getUpdatedAt() == null ? null : product.getUpdatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE);
                appendUrl(xml, base + "/products/" + product.getId(), updated, "weekly", "0.8");
            }
        }
        for (CmsPage page : cmsPageRepository.findAll()) {
            if (page.isActive() && isSitemapEnabled("CMS_PAGE", page.getId(), page.getSlug())) {
                String updated = page.getUpdatedAt() == null ? null : page.getUpdatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE);
                appendUrl(xml, base + "/" + page.getSlug(), updated, "monthly", "0.6");
            }
        }
        xml.append("</urlset>");
        return xml.toString();
    }

    @GetMapping(value = "/api/seo/settings", produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<SeoSettingResponse> seoSettings(
            @RequestParam String targetType,
            @RequestParam(required = false) Long targetId,
            @RequestParam(required = false) String targetSlug
    ) {
        return ApiResponse.ok("SEO settings fetched", seoSettingService.find(targetType, targetId, targetSlug).orElse(null));
    }

    @GetMapping(value = "/robots.txt", produces = MediaType.TEXT_PLAIN_VALUE)
    public String robots() {
        String base = websiteBaseUrl.replaceAll("/+$", "");
        return "User-agent: *\n"
                + "Allow: /\n"
                + "Disallow: /admin\n"
                + "Disallow: /login\n"
                + "Sitemap: " + base + "/sitemap.xml\n";
    }

    @GetMapping(value = "/api/seo/products/{id}/json-ld", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> productJsonLd(@PathVariable Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        String base = websiteBaseUrl.replaceAll("/+$", "");
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("@context", "https://schema.org");
        json.put("@type", "Product");
        json.put("name", product.getTitle());
        json.put("description", firstNonBlank(product.getSeoDescription(), product.getDescription()));
        json.put("sku", product.getSku());
        json.put("url", base + "/products/" + product.getId());
        if (!product.getImages().isEmpty()) {
            json.put("image", product.getImages().stream().map(image -> image.getImageUrl()).toList());
        }
        if (product.getBrand() != null) {
            json.put("brand", Map.of("@type", "Brand", "name", product.getBrand().getName()));
        }
        json.put("offers", Map.of(
                "@type", "Offer",
                "priceCurrency", "INR",
                "price", product.getPrice(),
                "availability", product.isAvailable() ? "https://schema.org/InStock" : "https://schema.org/OutOfStock",
                "itemCondition", "https://schema.org/RefurbishedCondition",
                "url", base + "/products/" + product.getId()
        ));
        return json;
    }

    @GetMapping(value = "/api/seo/categories/{id}/json-ld", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> categoryJsonLd(@PathVariable Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        String base = websiteBaseUrl.replaceAll("/+$", "");
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("@context", "https://schema.org");
        json.put("@type", "CollectionPage");
        json.put("name", firstNonBlank(category.getSeoTitle(), category.getName()));
        json.put("description", category.getSeoDescription());
        json.put("url", firstNonBlank(category.getCanonicalUrl(), base + "/products?categoryId=" + category.getId()));
        if (category.getOgImageUrl() != null && !category.getOgImageUrl().isBlank()) {
            json.put("image", category.getOgImageUrl());
        }
        return json;
    }

    private void appendUrl(StringBuilder xml, String loc, String lastmod, String changefreq, String priority) {
        xml.append("  <url>\n");
        xml.append("    <loc>").append(escapeXml(loc)).append("</loc>\n");
        if (lastmod != null) {
            xml.append("    <lastmod>").append(lastmod).append("</lastmod>\n");
        }
        xml.append("    <changefreq>").append(changefreq).append("</changefreq>\n");
        xml.append("    <priority>").append(priority).append("</priority>\n");
        xml.append("  </url>\n");
    }

    private String escapeXml(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private String firstNonBlank(String first, String second) {
        return first != null && !first.isBlank() ? first : second;
    }

    private boolean isSitemapEnabled(String targetType, Long targetId, String targetSlug) {
        Optional<SeoSetting> setting = seoSettingService.findEntity(targetType, targetId, targetSlug);
        return setting.map(value -> value.isSitemapEnabled() && !value.isNoIndex()).orElse(true);
    }
}
