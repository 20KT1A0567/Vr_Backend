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

    @Column(length = 500)
    private String logoUrl;

    @Column(length = 500)
    private String faviconUrl;

    @Column(length = 180)
    private String tagline;

    @Column(length = 1000)
    private String footerDescription;

    private String supportEmail;

    private String supportPhone;

    @Column(length = 1000)
    private String shippingNote;

    @Column(nullable = false)
    private boolean pickupEnabled = true;

    @Column(nullable = false)
    private boolean deliveryEnabled = true;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal standardDeliveryCharge = BigDecimal.ZERO;

    @Column(precision = 10, scale = 2)
    private BigDecimal freeDeliveryThreshold;

    @Column(length = 2000)
    private String stateDeliveryCharges;

    @Column(length = 2000)
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

    @Column(length = 500)
    private String companyAddress;

    @Column(length = 20)
    private String companyPincode;

    @Column(length = 40, name = "invoice_prefix")
    private String invoicePrefix = "INV-";

    @Column(name = "invoice_next_sequence", nullable = false)
    private Long invoiceNextSequence = 1L;

    @Column(name = "invoice_padding", nullable = false)
    private Integer invoicePadding = 6;

    @Column(name = "invoice_terms", length = 2000)
    private String invoiceTerms;

    @Column(length = 1000)
    private String returnPolicy;

    private String defaultCity;

    private String defaultState;

    @Column(length = 500)
    private String mapLink;

    @Column(nullable = false)
    private boolean includeDefaultHomeSections = true;

    @Column(length = 500)
    private String defaultHomeSectionTypes = "TODAYS_DEALS,FEATURED_PRODUCTS,BEST_SELLERS,NEW_ARRIVALS,LOW_PRICE_DEALS";

    private String notificationEmailFrom;

    private String notificationReplyTo;

    private String whatsappNumber;

    @Column(length = 500)
    private String facebookUrl;

    @Column(length = 500)
    private String instagramUrl;

    @Column(length = 500)
    private String xUrl;

    @Column(length = 500)
    private String linkedinUrl;

    @Column(length = 500)
    private String youtubeUrl;

    @Column(name = "homepage_builder_json", columnDefinition = "LONGTEXT")
    private String homepageBuilderJson;

    @Column(nullable = false)
    private boolean orderNotificationsEnabled = true;

    @Column(nullable = false)
    private boolean paymentNotificationsEnabled = true;

    @Column(nullable = false)
    private boolean returnNotificationsEnabled = true;

    @Column(length = 1000)
    private String securityNotice;
}
