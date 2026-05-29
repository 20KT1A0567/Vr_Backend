package com.vrtechnologies.vrtech.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "site_settings")
public class SiteSettings extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String companyName;

    @Column(name = "logo_url", columnDefinition = "TEXT")
    private String logoUrl;

    @Column(name = "favicon_url", columnDefinition = "TEXT")
    private String faviconUrl;

    @Column(name = "tagline", columnDefinition = "TEXT")
    private String tagline;

    @Column(name = "footer_description", columnDefinition = "TEXT")
    private String footerDescription;

    private String supportEmail;

    private String supportPhone;

    @Column(name = "shipping_note", columnDefinition = "TEXT")
    private String shippingNote;

    @Column(nullable = false)
    private boolean pickupEnabled = true;

    @Column(nullable = false)
    private boolean deliveryEnabled = true;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal standardDeliveryCharge = BigDecimal.ZERO;

    @Column(precision = 10, scale = 2)
    private BigDecimal freeDeliveryThreshold;

    @Column(name = "state_delivery_charges", columnDefinition = "TEXT")
    private String stateDeliveryCharges;

    @Column(name = "state_delivery_windows", columnDefinition = "TEXT")
    private String stateDeliveryWindows;

    @Column(nullable = false)
    private Integer estimatedDeliveryDays = 5;

    @Column(nullable = false)
    private boolean gstEnabled = true;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal gstRate = BigDecimal.valueOf(18);

    @Column(length = 40)
    private String gstNumber;

    @Column(length = 20)
    private String companyPan;

    @Column(length = 20)
    private String defaultHsnCode;

    @Column(name = "company_address", columnDefinition = "TEXT")
    private String companyAddress;

    @Column(length = 20)
    private String companyPincode;

    @Column(length = 40, name = "invoice_prefix")
    private String invoicePrefix = "INV-";

    @Column(name = "invoice_next_sequence", nullable = false)
    private Long invoiceNextSequence = 1L;

    @Column(name = "invoice_padding", nullable = false)
    private Integer invoicePadding = 6;

    @Column(name = "invoice_terms", columnDefinition = "TEXT")
    private String invoiceTerms;

    @Column(name = "return_policy", columnDefinition = "TEXT")
    private String returnPolicy;

    private String defaultCity;

    private String defaultState;

    @Column(name = "map_link", columnDefinition = "TEXT")
    private String mapLink;

    @Column(nullable = false)
    private boolean includeDefaultHomeSections = true;

    @Column(name = "default_home_section_types", columnDefinition = "TEXT")
    private String defaultHomeSectionTypes = "TODAYS_DEALS,FEATURED_PRODUCTS,BEST_SELLERS,NEW_ARRIVALS,LOW_PRICE_DEALS";

    private String notificationEmailFrom;

    private String notificationReplyTo;

    private String whatsappNumber;

    @Column(name = "facebook_url", columnDefinition = "TEXT")
    private String facebookUrl;

    @Column(name = "instagram_url", columnDefinition = "TEXT")
    private String instagramUrl;

    @Column(name = "x_url", columnDefinition = "TEXT")
    private String xUrl;

    @Column(name = "linkedin_url", columnDefinition = "TEXT")
    private String linkedinUrl;

    @Column(name = "youtube_url", columnDefinition = "TEXT")
    private String youtubeUrl;

    @Column(name = "homepage_builder_json", columnDefinition = "LONGTEXT")
    private String homepageBuilderJson;

    @Column(nullable = false)
    private boolean orderNotificationsEnabled = true;

    @Column(nullable = false)
    private boolean paymentNotificationsEnabled = true;

    @Column(nullable = false)
    private boolean returnNotificationsEnabled = true;

    @Column(name = "security_notice", columnDefinition = "TEXT")
    private String securityNotice;

    @Column(name = "admin_allowed_ips", columnDefinition = "TEXT")
    private String adminAllowedIps;

    @Column(name = "invoice_layout", length = 30, nullable = false)
    private String invoiceLayout = "MINIMAL";

    @Column(name = "invoice_format", length = 30, nullable = false)
    private String invoiceFormat = "PDF";

    @Column(name = "maintenance_mode_active", nullable = false)
    private boolean maintenanceModeActive = false;
}
