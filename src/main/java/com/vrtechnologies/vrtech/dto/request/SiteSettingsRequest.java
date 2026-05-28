package com.vrtechnologies.vrtech.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class SiteSettingsRequest {

    @NotBlank
    private String companyName;

    private String logoUrl;
    private String faviconUrl;
    private String tagline;
    private String footerDescription;
    private String supportEmail;

    private String supportPhone;
    private String shippingNote;
    private Boolean pickupEnabled;
    private Boolean deliveryEnabled;
    private BigDecimal standardDeliveryCharge;
    private BigDecimal freeDeliveryThreshold;
    private String stateDeliveryCharges;
    private String stateDeliveryWindows;
    private Integer estimatedDeliveryDays;
    private Boolean gstEnabled;
    private BigDecimal gstRate;
    private String gstNumber;
    private String companyPan;
    private String defaultHsnCode;
    private String companyAddress;
    private String companyPincode;
    private String invoicePrefix;
    private Integer invoicePadding;
    private Long invoiceNextSequence;
    private String invoiceTerms;
    private String returnPolicy;
    private String defaultCity;
    private String defaultState;
    private String mapLink;
    private Boolean includeDefaultHomeSections;
    private String defaultHomeSectionTypes;
    private String notificationEmailFrom;
    private String notificationReplyTo;
    private String whatsappNumber;
    private String facebookUrl;
    private String instagramUrl;
    private String xUrl;
    private String linkedinUrl;
    private String youtubeUrl;
    private String homepageBuilderJson;
    private Boolean orderNotificationsEnabled;
    private Boolean paymentNotificationsEnabled;
    private Boolean returnNotificationsEnabled;
    private String securityNotice;
    private String adminAllowedIps;
    private String invoiceLayout;
    private String invoiceFormat;
}
