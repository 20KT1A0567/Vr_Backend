package com.vrtechnologies.vrtech.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vrtechnologies.vrtech.dto.request.CmsPageFaqItemRequest;
import com.vrtechnologies.vrtech.dto.request.CmsPageRequest;
import com.vrtechnologies.vrtech.dto.request.CmsPageSectionRequest;
import com.vrtechnologies.vrtech.dto.response.CmsPageFaqItemResponse;
import com.vrtechnologies.vrtech.dto.response.CmsPageResponse;
import com.vrtechnologies.vrtech.dto.response.CmsPageSectionResponse;
import com.vrtechnologies.vrtech.entity.CmsPage;
import com.vrtechnologies.vrtech.exception.ResourceNotFoundException;
import com.vrtechnologies.vrtech.repository.CmsPageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class CmsPageService {

    private static final List<String> DEFAULT_PAGE_ORDER = List.of(
            "about",
            "privacy",
            "terms",
            "shipping",
            "warranty",
            "returns",
            "contact",
            "faq"
    );

    private static final TypeReference<List<CmsPageSectionResponse>> SECTION_LIST_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<List<CmsPageFaqItemResponse>> FAQ_LIST_TYPE = new TypeReference<>() {
    };

    private final CmsPageRepository cmsPageRepository;
    private final ObjectMapper objectMapper;

    public CmsPageService(CmsPageRepository cmsPageRepository, ObjectMapper objectMapper) {
        this.cmsPageRepository = cmsPageRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public List<CmsPageResponse> getAllPages() {
        ensureDefaultPages();
        return cmsPageRepository.findAll().stream()
                .sorted(Comparator.comparingInt(page -> pageOrder(page.getSlug())))
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public CmsPageResponse getPage(String slug, boolean adminView) {
        ensureDefaultPages();
        CmsPage page = cmsPageRepository.findBySlug(normalizeSlug(slug))
                .orElseThrow(() -> new ResourceNotFoundException("CMS page not found"));
        if (!adminView && !page.isActive()) {
            throw new ResourceNotFoundException("CMS page not found");
        }
        return toResponse(page);
    }

    @Transactional
    public CmsPageResponse savePage(String slug, CmsPageRequest request) {
        ensureDefaultPages();
        CmsPage page = cmsPageRepository.findBySlug(normalizeSlug(slug))
                .orElseThrow(() -> new ResourceNotFoundException("CMS page not found"));
        page.setTitle(request.getTitle().trim());
        page.setMetaTitle(normalizeString(request.getMetaTitle()));
        page.setMetaDescription(normalizeString(request.getMetaDescription()));
        page.setEyebrow(normalizeString(request.getEyebrow()));
        page.setHeroTitle(normalizeString(request.getHeroTitle()));
        page.setHeroDescription(normalizeString(request.getHeroDescription()));
        page.setBody(normalizeString(request.getBody()));
        page.setActive(request.getActive() == null || request.getActive());
        page.setSectionsJson(writeSections(request.getSections()));
        page.setFaqItemsJson(writeFaqItems(request.getFaqItems()));
        return toResponse(cmsPageRepository.save(page));
    }

    @Transactional
    public void ensureDefaultPages() {
        Map<String, CmsPageSeed> seeds = defaultSeeds();
        List<CmsPage> missing = new ArrayList<>();
        for (Map.Entry<String, CmsPageSeed> entry : seeds.entrySet()) {
            if (cmsPageRepository.findBySlug(entry.getKey()).isPresent()) {
                continue;
            }
            CmsPageSeed seed = entry.getValue();
            CmsPage page = new CmsPage();
            page.setSlug(entry.getKey());
            page.setTitle(seed.title());
            page.setMetaTitle(seed.metaTitle());
            page.setMetaDescription(seed.metaDescription());
            page.setEyebrow(seed.eyebrow());
            page.setHeroTitle(seed.heroTitle());
            page.setHeroDescription(seed.heroDescription());
            page.setBody(seed.body());
            page.setSectionsJson(writeSections(seed.sections()));
            page.setFaqItemsJson(writeFaqItems(seed.faqItems()));
            page.setActive(true);
            missing.add(page);
        }
        if (!missing.isEmpty()) {
            cmsPageRepository.saveAll(missing);
        }
    }

    private CmsPageResponse toResponse(CmsPage page) {
        return CmsPageResponse.builder()
                .id(page.getId())
                .slug(page.getSlug())
                .title(page.getTitle())
                .metaTitle(page.getMetaTitle())
                .metaDescription(page.getMetaDescription())
                .eyebrow(page.getEyebrow())
                .heroTitle(page.getHeroTitle())
                .heroDescription(page.getHeroDescription())
                .body(page.getBody())
                .active(page.isActive())
                .sections(readSections(page.getSectionsJson()))
                .faqItems(readFaqItems(page.getFaqItemsJson()))
                .updatedAt(page.getUpdatedAt())
                .build();
    }

    private List<CmsPageSectionResponse> readSections(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, SECTION_LIST_TYPE);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Failed to read CMS page sections", exception);
        }
    }

    private List<CmsPageFaqItemResponse> readFaqItems(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, FAQ_LIST_TYPE);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Failed to read CMS page FAQ items", exception);
        }
    }

    private String writeSections(List<? extends CmsPageSectionRequest> sections) {
        List<CmsPageSectionResponse> normalized = new ArrayList<>();
        if (sections != null) {
            for (CmsPageSectionRequest section : sections) {
                String title = normalizeString(section.getTitle());
                String content = normalizeString(section.getContent());
                if (title == null && content == null) {
                    continue;
                }
                normalized.add(CmsPageSectionResponse.builder()
                        .title(title)
                        .content(content)
                        .build());
            }
        }
        return writeJson(normalized);
    }

    private String writeFaqItems(List<? extends CmsPageFaqItemRequest> faqItems) {
        List<CmsPageFaqItemResponse> normalized = new ArrayList<>();
        if (faqItems != null) {
            for (CmsPageFaqItemRequest faqItem : faqItems) {
                String question = normalizeString(faqItem.getQuestion());
                String answer = normalizeString(faqItem.getAnswer());
                if (question == null && answer == null) {
                    continue;
                }
                normalized.add(CmsPageFaqItemResponse.builder()
                        .question(question)
                        .answer(answer)
                        .build());
            }
        }
        return writeJson(normalized);
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Failed to serialize CMS page data", exception);
        }
    }

    private String normalizeString(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String normalizeSlug(String slug) {
        return slug == null ? "" : slug.trim().toLowerCase(Locale.ROOT);
    }

    private int pageOrder(String slug) {
        int index = DEFAULT_PAGE_ORDER.indexOf(slug);
        return index >= 0 ? index : Integer.MAX_VALUE;
    }

    private Map<String, CmsPageSeed> defaultSeeds() {
        Map<String, CmsPageSeed> seeds = new LinkedHashMap<>();
        seeds.put("about", new CmsPageSeed(
                "About Us",
                "About VR Technologies",
                "Learn about VR Technologies and how we support refurbished system buyers with store-backed service.",
                "Company",
                "About VR Technologies",
                "VR Technologies supplies certified refurbished laptops, desktops, workstations, monitors, and accessories with store-backed support.",
                "We focus on practical business and student systems with transparent condition details, real store availability, and branch-backed follow-up before and after purchase.",
                List.of(
                        section("What we sell", "We focus on practical business and student systems: refurbished laptops, desktops, MacBooks, workstations, monitors, chargers, SSDs, RAM, and accessories."),
                        section("How we build trust", "Every eligible product is quality checked, listed with clear condition details, mapped to real store availability, and supported through branch contact channels."),
                        section("Store-backed service", "Customers can use online ordering, store pickup, WhatsApp support, and branch follow-up for warranty, delivery, and product questions.")
                ),
                List.of()
        ));
        seeds.put("privacy", new CmsPageSeed(
                "Privacy Policy",
                "Privacy Policy",
                "Review how VR Technologies uses customer information for orders, support, and communication.",
                "Privacy",
                "Privacy Policy",
                "We collect only the information needed to run orders, support, customer accounts, and store communication.",
                "Your data is used to fulfill orders, manage account access, and provide support across online and branch-assisted journeys.",
                List.of(
                        section("Information used", "Name, phone, email, delivery address, cart, wishlist, and order details may be used to process orders and provide support."),
                        section("Security", "Account access uses authenticated sessions. Customers should keep login details private and contact support if something looks wrong."),
                        section("Communication", "We may contact customers for order confirmation, delivery, warranty, returns, and support through phone, email, or WhatsApp.")
                ),
                List.of()
        ));
        seeds.put("terms", new CmsPageSeed(
                "Terms & Conditions",
                "Terms and Conditions",
                "Review the core terms governing pricing, product listings, orders, and fulfillment support.",
                "Terms",
                "Terms and Conditions",
                "These terms describe how product information, pricing, ordering, and store support work on the VR Technologies website.",
                "Orders, pricing, and stock are managed against live branch availability and are subject to final operational confirmation.",
                List.of(
                        section("Product information", "Refurbished product stock, condition, price, and accessories can vary by branch. Final confirmation happens at checkout or store verification."),
                        section("Pricing", "Prices and discounts may change without prior notice. Confirmed orders retain the order amount unless a correction is required due to stock or listing error."),
                        section("Orders", "Orders are accepted after customer details, payment method, and store availability are confirmed. VR Technologies may cancel orders that cannot be fulfilled.")
                ),
                List.of()
        ));
        seeds.put("shipping", new CmsPageSeed(
                "Shipping Policy",
                "Shipping and Store Pickup",
                "Understand delivery timelines, pickup readiness, and address accuracy expectations.",
                "Delivery",
                "Shipping and Store Pickup",
                "Choose delivery or pickup where available. Store mapping helps customers know where support and fulfillment come from.",
                "Most orders move through a branch-aware process that balances local inventory, pickup readiness, and doorstep delivery.",
                List.of(
                        section("Delivery estimate", "Most local orders are prepared after store confirmation. Delivery timelines depend on product availability, address, and store operations."),
                        section("Store pickup", "Pickup orders can be collected from the selected branch after confirmation. Please carry order details and contact information."),
                        section("Address accuracy", "Customers are responsible for providing a reachable phone number and accurate delivery address during checkout.")
                ),
                List.of()
        ));
        seeds.put("warranty", new CmsPageSeed(
                "Warranty Policy",
                "Warranty Policy",
                "Review warranty coverage, exclusions, and what customers should bring for support.",
                "Support",
                "Warranty Policy",
                "Warranty coverage depends on the product listing and invoice, with carry-in support through VR Technologies branches.",
                "Warranty information should align with the product page, invoice, and branch support process for each eligible item.",
                List.of(
                        section("Coverage", "Eligible refurbished products include the warranty duration shown on the product page and invoice. Accessories may have different warranty periods."),
                        section("What to bring", "Carry the invoice, product, charger or accessory if relevant, and a short description of the issue when visiting the store."),
                        section("Exclusions", "Physical damage, liquid damage, unauthorized repair, software misuse, and consumable wear are not covered unless explicitly stated by the store.")
                ),
                List.of()
        ));
        seeds.put("returns", new CmsPageSeed(
                "Returns Policy",
                "Returns and Refunds",
                "Review the return window, inspection flow, and refund handling expectations.",
                "Returns",
                "Returns and Refunds",
                "Return support is designed to be simple and branch-aware for eligible products.",
                "Return eligibility depends on the product listing, timing of the request, and condition of the returned item after inspection.",
                List.of(
                        section("Return window", "Eligible products show the return window on the product page. Return requests should be raised quickly with the original invoice and product condition intact."),
                        section("Inspection", "Returned products are checked by the store team before approval. Refund or replacement depends on product condition and issue verification."),
                        section("Refund mode", "Approved refunds are processed through the original or agreed payment method. Offline payments may be settled directly by the fulfillment branch.")
                ),
                List.of()
        ));
        seeds.put("contact", new CmsPageSeed(
                "Contact Page",
                "Contact Us",
                "Get in touch for product enquiries, bulk orders, warranty support, or general assistance.",
                "Human Support",
                "Talk to us before you buy.",
                "Reach out for product guidance, branch support, or bulk procurement conversations. The team can guide customers before and after purchase.",
                "Use the enquiry forms below for retail questions or B2B requirements. Your admin team can update this page from the CMS without code changes.",
                List.of(
                        section("General support", "Customers can use this page for model guidance, warranty clarifications, store coordination, and general pre-sale or post-sale help."),
                        section("Bulk and B2B", "Businesses, institutes, and teams can share quantity, budget, model preference, and rollout timeline through the bulk enquiry form."),
                        section("Response process", "Most enquiries are triaged by the admin team and routed to the right branch or support owner for follow-up.")
                ),
                List.of(
                        faq("How quickly will someone respond?", "Most enquiries are reviewed during business hours and routed to the relevant team as quickly as possible."),
                        faq("Can I ask for a bulk quote?", "Yes. Use the bulk or B2B enquiry form with quantity, specs, and budget details."),
                        faq("Can I contact a specific store?", "Yes. Store contact details remain available through the website store pages and support channels.")
                )
        ));
        seeds.put("faq", new CmsPageSeed(
                "FAQ Page",
                "Frequently Asked Questions",
                "Find quick answers about products, ordering, delivery, warranty, and returns.",
                "Support",
                "Frequently Asked Questions",
                "Quick answers to the questions customers ask most before placing an order.",
                "Keep this page updated from the admin panel as your operations, policies, or fulfillment process evolves.",
                List.of(
                        section("Products", "Use this section to explain condition grades, accessories included, stock visibility, or store availability expectations."),
                        section("Orders and delivery", "Use this area to explain checkout, store pickup, delivery timelines, and payment support."),
                        section("Warranty and returns", "Clarify warranty coverage, return eligibility, and the support process customers should expect.")
                ),
                List.of(
                        faq("Are the products refurbished?", "Yes. Product condition, warranty, and included accessories should be clearly listed on each product page."),
                        faq("Can I pick up from a store?", "Yes, when pickup is enabled and the selected store is available for the order."),
                        faq("Do you offer warranty support?", "Eligible products include the warranty duration shown on the product page and invoice."),
                        faq("How do returns work?", "Return requests should be raised within the eligible window shown on the product page or order support flow.")
                )
        ));
        return seeds;
    }

    private CmsPageSectionRequest section(String title, String content) {
        CmsPageSectionRequest section = new CmsPageSectionRequest();
        section.setTitle(title);
        section.setContent(content);
        return section;
    }

    private CmsPageFaqItemRequest faq(String question, String answer) {
        CmsPageFaqItemRequest item = new CmsPageFaqItemRequest();
        item.setQuestion(question);
        item.setAnswer(answer);
        return item;
    }

    private record CmsPageSeed(
            String title,
            String metaTitle,
            String metaDescription,
            String eyebrow,
            String heroTitle,
            String heroDescription,
            String body,
            List<CmsPageSectionRequest> sections,
            List<CmsPageFaqItemRequest> faqItems
    ) {
    }
}
