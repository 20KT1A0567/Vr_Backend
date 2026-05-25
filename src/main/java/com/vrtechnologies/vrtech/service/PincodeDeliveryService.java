package com.vrtechnologies.vrtech.service;

import com.vrtechnologies.vrtech.dto.request.PincodeDeliveryRuleRequest;
import com.vrtechnologies.vrtech.dto.response.PincodeDeliveryCheckResponse;
import com.vrtechnologies.vrtech.dto.response.PincodeDeliveryRuleResponse;
import com.vrtechnologies.vrtech.dto.response.ProductImportResponse;
import com.vrtechnologies.vrtech.entity.PincodeDeliveryRule;
import com.vrtechnologies.vrtech.entity.SiteSettings;
import com.vrtechnologies.vrtech.entity.Store;
import com.vrtechnologies.vrtech.entity.PincodeZone;
import com.vrtechnologies.vrtech.entity.PincodeBlacklist;
import com.vrtechnologies.vrtech.entity.Holiday;
import com.vrtechnologies.vrtech.entity.PincodeLookupLog;
import com.vrtechnologies.vrtech.entity.PincodeApiCache;
import com.vrtechnologies.vrtech.exception.BadRequestException;
import com.vrtechnologies.vrtech.exception.ResourceNotFoundException;
import com.vrtechnologies.vrtech.repository.PincodeDeliveryRuleRepository;
import com.vrtechnologies.vrtech.repository.StoreRepository;
import com.vrtechnologies.vrtech.repository.PincodeZoneRepository;
import com.vrtechnologies.vrtech.repository.PincodeBlacklistRepository;
import com.vrtechnologies.vrtech.repository.HolidayRepository;
import com.vrtechnologies.vrtech.repository.PincodeLookupLogRepository;
import com.vrtechnologies.vrtech.repository.PincodeApiCacheRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;
import java.net.HttpURLConnection;
import java.io.IOException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

@Service
public class PincodeDeliveryService {

    private final PincodeDeliveryRuleRepository pincodeDeliveryRuleRepository;
    private final StoreRepository storeRepository;
    private final PincodeZoneRepository pincodeZoneRepository;
    private final PincodeBlacklistRepository pincodeBlacklistRepository;
    private final HolidayRepository holidayRepository;
    private final PincodeLookupLogRepository pincodeLookupLogRepository;
    private final PincodeApiCacheRepository pincodeApiCacheRepository;

    public PincodeDeliveryService(PincodeDeliveryRuleRepository pincodeDeliveryRuleRepository,
                                  StoreRepository storeRepository,
                                  PincodeZoneRepository pincodeZoneRepository,
                                  PincodeBlacklistRepository pincodeBlacklistRepository,
                                  HolidayRepository holidayRepository,
                                  PincodeLookupLogRepository pincodeLookupLogRepository,
                                  PincodeApiCacheRepository pincodeApiCacheRepository) {
        this.pincodeDeliveryRuleRepository = pincodeDeliveryRuleRepository;
        this.storeRepository = storeRepository;
        this.pincodeZoneRepository = pincodeZoneRepository;
        this.pincodeBlacklistRepository = pincodeBlacklistRepository;
        this.holidayRepository = holidayRepository;
        this.pincodeLookupLogRepository = pincodeLookupLogRepository;
        this.pincodeApiCacheRepository = pincodeApiCacheRepository;
    }

    public List<PincodeDeliveryRuleResponse> getRules() {
        return pincodeDeliveryRuleRepository.findAllByOrderByPincodeAscPriorityAscIdAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public PincodeDeliveryRuleResponse saveRule(PincodeDeliveryRuleRequest request, Long id) {
        PincodeDeliveryRule rule = id == null
                ? new PincodeDeliveryRule()
                : pincodeDeliveryRuleRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Pincode delivery rule not found"));

        String normalizedPincode = normalizePincode(request.getPincode());
        Store store = request.getStoreId() == null ? null : storeRepository.findById(request.getStoreId())
                .filter(Store::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("Store not found"));

        rule.setPincode(normalizedPincode);
        rule.setCountryCode(normalizeCountryCode(request.getCountryCode()));
        rule.setStateName(trimToNull(request.getStateName()));
        rule.setCityName(trimToNull(request.getCityName()));
        rule.setZoneName(trimToNull(request.getZoneName()));
        rule.setServiceable(request.getServiceable() == null || request.getServiceable());
        rule.setCodAvailable(request.getCodAvailable() == null || request.getCodAvailable());
        rule.setPrepaidAvailable(request.getPrepaidAvailable() == null || request.getPrepaidAvailable());
        rule.setDeliveryCharge(nonNegative(request.getDeliveryCharge()));
        rule.setFreeDeliveryThreshold(request.getFreeDeliveryThreshold() == null ? null : nonNegative(request.getFreeDeliveryThreshold()));
        int minDays = sanitizeDayValue(request.getMinDeliveryDays(), 1);
        int maxDays = sanitizeDayValue(request.getMaxDeliveryDays(), minDays);
        if (maxDays < minDays) {
            maxDays = minDays;
        }
        rule.setMinDeliveryDays(minDays);
        rule.setMaxDeliveryDays(maxDays);
        rule.setStore(store);
        rule.setPriority(request.getPriority() == null ? 100 : Math.max(0, request.getPriority()));
        rule.setActive(request.getActive() == null || request.getActive());
        rule.setNotes(trimToNull(request.getNotes()));

        return toResponse(pincodeDeliveryRuleRepository.save(rule));
    }

    @Transactional
    public void deleteRule(Long id) {
        PincodeDeliveryRule rule = pincodeDeliveryRuleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pincode delivery rule not found"));
        pincodeDeliveryRuleRepository.delete(rule);
    }

    public String exportCsv() {
        StringBuilder csv = new StringBuilder("id,pincode,countryCode,stateName,cityName,zoneName,serviceable,codAvailable,prepaidAvailable,deliveryCharge,freeDeliveryThreshold,minDeliveryDays,maxDeliveryDays,storeId,priority,active,notes\n");
        for (PincodeDeliveryRule rule : pincodeDeliveryRuleRepository.findAllByOrderByPincodeAscPriorityAscIdAsc()) {
            csv.append(rule.getId()).append(',')
                    .append(escape(rule.getPincode())).append(',')
                    .append(escape(rule.getCountryCode())).append(',')
                    .append(escape(rule.getStateName())).append(',')
                    .append(escape(rule.getCityName())).append(',')
                    .append(escape(rule.getZoneName())).append(',')
                    .append(rule.isServiceable()).append(',')
                    .append(rule.isCodAvailable()).append(',')
                    .append(rule.isPrepaidAvailable()).append(',')
                    .append(rule.getDeliveryCharge()).append(',')
                    .append(rule.getFreeDeliveryThreshold() == null ? "" : rule.getFreeDeliveryThreshold()).append(',')
                    .append(rule.getMinDeliveryDays()).append(',')
                    .append(rule.getMaxDeliveryDays()).append(',')
                    .append(rule.getStore() == null ? "" : rule.getStore().getId()).append(',')
                    .append(rule.getPriority()).append(',')
                    .append(rule.isActive()).append(',')
                    .append(escape(rule.getNotes()))
                    .append('\n');
        }
        return csv.toString();
    }

    @Transactional
    public ProductImportResponse importCsv(MultipartFile file) {
        int created = 0;
        int updated = 0;
        int skipped = 0;
        List<String> messages = new ArrayList<>();
        try {
            String content = new String(file.getBytes(), StandardCharsets.UTF_8);
            List<String> lines = content.lines().toList();
            if (lines.isEmpty()) {
                return ProductImportResponse.builder().created(0).updated(0).skipped(0).messages(List.of("CSV file is empty.")).build();
            }

            Map<String, Integer> headerIndex = headerIndex(lines.get(0));
            for (int row = 1; row < lines.size(); row++) {
                String line = lines.get(row).trim();
                if (line.isEmpty()) {
                    continue;
                }

                try {
                    List<String> cells = parseCsvLine(line);
                    String pincode = cell(cells, headerIndex, "pincode");
                    if (pincode == null || pincode.isBlank()) {
                        skipped++;
                        messages.add("Row " + (row + 1) + ": skipped because pincode is blank.");
                        continue;
                    }

                    Long storeId = parseLong(cell(cells, headerIndex, "storeid"));
                    Optional<PincodeDeliveryRule> existing = storeId == null
                            ? pincodeDeliveryRuleRepository.findFirstByPincodeAndStoreIsNullOrderByIdAsc(normalizePincode(pincode))
                            : pincodeDeliveryRuleRepository.findFirstByPincodeAndStore_IdOrderByIdAsc(normalizePincode(pincode), storeId);

                    PincodeDeliveryRuleRequest request = new PincodeDeliveryRuleRequest();
                    request.setPincode(pincode);
                    request.setCountryCode(cell(cells, headerIndex, "countrycode"));
                    request.setStateName(cell(cells, headerIndex, "statename"));
                    request.setCityName(cell(cells, headerIndex, "cityname"));
                    request.setZoneName(cell(cells, headerIndex, "zonename"));
                    request.setServiceable(parseBoolean(cell(cells, headerIndex, "serviceable")));
                    request.setCodAvailable(parseBoolean(cell(cells, headerIndex, "codavailable")));
                    request.setPrepaidAvailable(parseBoolean(cell(cells, headerIndex, "prepaidavailable")));
                    request.setDeliveryCharge(parseDecimal(cell(cells, headerIndex, "deliverycharge")));
                    request.setFreeDeliveryThreshold(parseDecimal(cell(cells, headerIndex, "freedeliverythreshold")));
                    request.setMinDeliveryDays(parseInteger(cell(cells, headerIndex, "mindeliverydays")));
                    request.setMaxDeliveryDays(parseInteger(cell(cells, headerIndex, "maxdeliverydays")));
                    request.setStoreId(storeId);
                    request.setPriority(parseInteger(cell(cells, headerIndex, "priority")));
                    request.setActive(parseBoolean(cell(cells, headerIndex, "active")));
                    request.setNotes(cell(cells, headerIndex, "notes"));
                    saveRule(request, existing.map(PincodeDeliveryRule::getId).orElse(null));
                    if (existing.isPresent()) {
                        updated++;
                    } else {
                        created++;
                    }
                } catch (Exception rowError) {
                    skipped++;
                    messages.add("Row " + (row + 1) + ": " + rowError.getMessage());
                }
            }
        } catch (Exception exception) {
            messages.add("Import failed: " + exception.getMessage());
        }
        return ProductImportResponse.builder().created(created).updated(updated).skipped(skipped).messages(messages).build();
    }

    public PincodeDeliveryCheckResponse checkPincode(String pincode, BigDecimal subtotal, Long storeId, Boolean codRequested, String deliveryState, SiteSettings settings) {
        String normalizedPincode = normalizePincode(pincode);
        DeliveryResolution resolution = resolveDelivery(settings, normalizedPincode, deliveryState, subtotal, storeId, Boolean.TRUE.equals(codRequested));

        // Find location info (using the resolved location from delivery resolution to avoid calling resolveLocation twice)
        PincodeLocation loc = resolution.getLocation();
        if (loc == null) {
            try {
                // If it was skipped by prefix match, only check local cache to avoid external API calls
                Optional<PincodeApiCache> cached = pincodeApiCacheRepository.findById(normalizedPincode);
                if (cached.isPresent()) {
                    PincodeApiCache cache = cached.get();
                    loc = new PincodeLocation(normalizedPincode, cache.getStateName(), cache.getDistrictName(), cache.getCityName());
                }
            } catch (Exception ignored) {}
        }

        String stateName = loc != null ? loc.getStateName() : null;
        String districtName = loc != null ? loc.getDistrictName() : null;
        String cityName = loc != null ? loc.getCityName() : null;

        // Log lookup in the database
        try {
            PincodeLookupLog logEntry = new PincodeLookupLog();
            logEntry.setPincode(normalizedPincode);
            logEntry.setStateName(stateName);
            logEntry.setDistrictName(districtName);
            logEntry.setCityName(cityName);
            logEntry.setServiceable(resolution.isServiceable());
            logEntry.setRuleSource(resolution.getRuleSource());
            pincodeLookupLogRepository.save(logEntry);
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(PincodeDeliveryService.class)
                    .error("Failed to log pincode lookup: {}", e.getMessage());
        }

        // Calculate expected dates (skipping weekends & holidays)
        LocalDate expectedMinDate = null;
        LocalDate expectedMaxDate = null;
        if (resolution.isServiceable() && resolution.getMinDeliveryDays() != null) {
            LocalDate baseDate = LocalDate.now();
            if (java.time.LocalTime.now().getHour() >= 18) { // 18:00 cutoff
                baseDate = baseDate.plusDays(1);
            }
            expectedMinDate = addBusinessDays(baseDate, resolution.getMinDeliveryDays());
            expectedMaxDate = addBusinessDays(baseDate, resolution.getMaxDeliveryDays() != null ? resolution.getMaxDeliveryDays() : resolution.getMinDeliveryDays());
        }

        DateTimeFormatter dtf = DateTimeFormatter.ISO_LOCAL_DATE;

        return PincodeDeliveryCheckResponse.builder()
                .pincode(normalizedPincode)
                .serviceable(resolution.isServiceable())
                .deliveryEnabled(settings == null || settings.isDeliveryEnabled())
                .codAvailable(resolution.isCodAvailable())
                .prepaidAvailable(resolution.isPrepaidAvailable())
                .deliveryCharge(resolution.getDeliveryCharge())
                .freeDeliveryThreshold(resolution.getFreeDeliveryThreshold())
                .freeDeliveryApplied(resolution.isFreeDeliveryApplied())
                .minDeliveryDays(resolution.getMinDeliveryDays())
                .maxDeliveryDays(resolution.getMaxDeliveryDays())
                .estimatedLabel(estimatedLabel(resolution.getMinDeliveryDays(), resolution.getMaxDeliveryDays()))
                .expectedMinDate(expectedMinDate != null ? expectedMinDate.format(dtf) : null)
                .expectedMaxDate(expectedMaxDate != null ? expectedMaxDate.format(dtf) : null)
                .storeId(resolution.getStoreId())
                .storeName(resolution.getStoreName())
                .ruleId(resolution.getRuleId())
                .ruleSource(resolution.getRuleSource())
                .message(resolution.getMessage())
                .build();
    }

    public DeliveryResolution resolveDelivery(SiteSettings settings, String pincode, String deliveryState, BigDecimal subtotal, Long storeId, boolean codRequested) {
        BigDecimal safeSubtotal = subtotal == null ? BigDecimal.ZERO : subtotal.max(BigDecimal.ZERO);
        if (settings != null && !settings.isDeliveryEnabled()) {
            return DeliveryResolution.unserviceable("SYSTEM", "Delivery is currently disabled");
        }

        String normalizedPincode = trimToNull(pincode);
        if (normalizedPincode != null) {
            normalizedPincode = normalizePincode(normalizedPincode);
        } else {
            return DeliveryResolution.unserviceable("PINCODE", "Pincode is required");
        }

        // 1. Blacklist Check
        Optional<PincodeBlacklist> blacklistOpt = pincodeBlacklistRepository.findByPincodeAndActiveTrue(normalizedPincode);
        if (blacklistOpt.isPresent()) {
            return DeliveryResolution.unserviceable("BLACKLIST", "Delivery is not available for this pincode (Blacklisted)");
        }

        // 2. Specific Override Check
        List<PincodeDeliveryRule> candidates = pincodeDeliveryRuleRepository.findByPincodeAndActiveTrueOrderByPriorityAscIdAsc(normalizedPincode);
        PincodeDeliveryRule best = pickBestRule(candidates, storeId);
        if (best != null) {
            return fromRule(best, safeSubtotal, codRequested, settings);
        }

        // 3. Prefix Zone Check (No API Dependency)
        List<PincodeZone> zones = pincodeZoneRepository.findByActiveTrueOrderByPriorityAscIdAsc();
        PincodeZone matchedPrefixZone = null;
        for (PincodeZone zone : zones) {
            if ("PINCODE_PREFIX".equalsIgnoreCase(zone.getMatchType())) {
                if (normalizedPincode.startsWith(zone.getMatchValue().trim())) {
                    matchedPrefixZone = zone;
                    break;
                }
            }
        }

        if (matchedPrefixZone != null) {
            DeliveryResolution resolution = fromZone(matchedPrefixZone, safeSubtotal, codRequested, settings);
            // Attach location info if already in local cache
            try {
                Optional<PincodeApiCache> cached = pincodeApiCacheRepository.findById(normalizedPincode);
                if (cached.isPresent()) {
                    PincodeApiCache cache = cached.get();
                    resolution.setLocation(new PincodeLocation(normalizedPincode, cache.getStateName(), cache.getDistrictName(), cache.getCityName()));
                }
            } catch (Exception ignored) {}
            return resolution;
        }

        // 4. Location Lookup (Cache or India Post API) - only if no prefix match
        PincodeLocation location = resolveLocation(normalizedPincode);

        // 5. State / District Zone Check
        for (PincodeZone zone : zones) {
            if (!"PINCODE_PREFIX".equalsIgnoreCase(zone.getMatchType()) && location != null) {
                if (matchesZone(zone, location)) {
                    DeliveryResolution resolution = fromZone(zone, safeSubtotal, codRequested, settings);
                    resolution.setLocation(location);
                    return resolution;
                }
            }
        }

        // 6. Fallback Check (State settings)
        String fallbackState = deliveryState;
        if (location != null && location.getStateName() != null) {
            fallbackState = location.getStateName();
        }
        if (settings != null && fallbackState != null && !fallbackState.isBlank()) {
            DeliveryResolution resolution = fromStateFallback(settings, safeSubtotal, fallbackState);
            resolution.setLocation(location);
            return resolution;
        }

        DeliveryResolution resolution = DeliveryResolution.unserviceable("PINCODE", "This pincode is not serviceable yet");
        resolution.setLocation(location);
        return resolution;
    }

    private LocalDate addBusinessDays(LocalDate baseDate, int daysToAdd) {
        LocalDate date = baseDate;
        int remaining = Math.max(0, daysToAdd);
        while (remaining > 0) {
            date = date.plusDays(1);
            int dayOfWeek = date.getDayOfWeek().getValue();
            boolean isWeekend = (dayOfWeek == 6 || dayOfWeek == 7);
            boolean isHoliday = holidayRepository.existsByHolidayDateAndActiveTrue(date);
            if (!isWeekend && !isHoliday) {
                remaining--;
            }
        }
        return date;
    }

    private boolean matchesZone(PincodeZone zone, PincodeLocation location) {
        String matchValue = zone.getMatchValue().trim();
        if ("PINCODE_PREFIX".equalsIgnoreCase(zone.getMatchType())) {
            return location.getPincode().startsWith(matchValue);
        } else if ("DISTRICT".equalsIgnoreCase(zone.getMatchType())) {
            return location.getDistrictName().equalsIgnoreCase(matchValue);
        } else if ("STATE".equalsIgnoreCase(zone.getMatchType())) {
            return location.getStateName().equalsIgnoreCase(matchValue);
        }
        return false;
    }

    private DeliveryResolution fromZone(PincodeZone zone, BigDecimal subtotal, boolean codRequested, SiteSettings settings) {
        if (!zone.isServiceable()) {
            return DeliveryResolution.unserviceable("ZONE:" + zone.getId(), "Delivery is not available for this zone");
        }
        if (codRequested && !zone.isCodAvailable()) {
            return DeliveryResolution.unserviceable("ZONE:" + zone.getId(), "Cash on delivery is not available for this zone");
        }
        if (!codRequested && !zone.isPrepaidAvailable()) {
            return DeliveryResolution.unserviceable("ZONE:" + zone.getId(), "Prepaid delivery is not available for this zone");
        }

        BigDecimal threshold = zone.getFreeDeliveryThreshold() != null
                ? zone.getFreeDeliveryThreshold()
                : settings == null ? null : settings.getFreeDeliveryThreshold();
        boolean freeApplied = threshold != null && threshold.compareTo(BigDecimal.ZERO) > 0 && subtotal.compareTo(threshold) >= 0;
        BigDecimal charge = freeApplied ? BigDecimal.ZERO : defaultDecimal(zone.getDeliveryCharge());
        return DeliveryResolution.serviceable(
                zone.isCodAvailable(),
                zone.isPrepaidAvailable(),
                charge.max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP),
                threshold,
                freeApplied,
                zone.getMinDeliveryDays(),
                zone.getMaxDeliveryDays(),
                "ZONE:" + zone.getId(),
                "Delivery available"
        );
    }

    private PincodeLocation resolveLocation(String pincode) {
        try {
            Optional<PincodeApiCache> cached = pincodeApiCacheRepository.findById(pincode);
            if (cached.isPresent()) {
                PincodeApiCache cache = cached.get();
                return new PincodeLocation(cache.getPincode(), cache.getStateName(), cache.getDistrictName(), cache.getCityName());
            }

            RestTemplate restTemplate = createSslIgnoringRestTemplate();
            String url = "https://api.postalpincode.in/pincode/" + pincode;
            PostOfficeResponse[] responses = restTemplate.getForObject(url, PostOfficeResponse[].class);

            if (responses != null && responses.length > 0) {
                PostOfficeResponse response = responses[0];
                if ("Success".equalsIgnoreCase(response.getStatus()) && response.getPostOffice() != null && !response.getPostOffice().isEmpty()) {
                    PostOfficeDetail detail = response.getPostOffice().get(0);
                    String stateName = detail.getState();
                    String districtName = detail.getDistrict();
                    String cityName = detail.getDivision();
                    if (cityName == null || cityName.isBlank()) {
                        cityName = detail.getName();
                    }

                    if (stateName != null && districtName != null && cityName != null) {
                        PincodeApiCache newCache = new PincodeApiCache();
                        newCache.setPincode(pincode);
                        newCache.setStateName(stateName.trim());
                        newCache.setDistrictName(districtName.trim());
                        newCache.setCityName(cityName.trim());
                        pincodeApiCacheRepository.save(newCache);

                        return new PincodeLocation(pincode, stateName.trim(), districtName.trim(), cityName.trim());
                    }
                }
            }
        } catch (Exception exception) {
            org.slf4j.LoggerFactory.getLogger(PincodeDeliveryService.class)
                    .warn("PostOffice API error for pincode {}: {}", pincode, exception.getMessage());
        }
        return null;
    }

    private RestTemplate createSslIgnoringRestTemplate() {
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() { return null; }
                public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                public void checkServerTrusted(X509Certificate[] certs, String authType) {}
            }}, new java.security.SecureRandom());

            SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory() {
                @Override
                protected void prepareConnection(HttpURLConnection connection, String httpMethod) throws IOException {
                    if (connection instanceof HttpsURLConnection) {
                        ((HttpsURLConnection) connection).setSSLSocketFactory(sslContext.getSocketFactory());
                        ((HttpsURLConnection) connection).setHostnameVerifier((hostname, session) -> true);
                    }
                    super.prepareConnection(connection, httpMethod);
                }
            };
            requestFactory.setConnectTimeout(1000); // 1.0s connect timeout
            requestFactory.setReadTimeout(1500);    // 1.5s read timeout
            return new RestTemplate(requestFactory);
        } catch (Exception e) {
            return new RestTemplate();
        }
    }

    public static class PincodeLocation {
        private final String pincode;
        private final String stateName;
        private final String districtName;
        private final String cityName;

        public PincodeLocation(String pincode, String stateName, String districtName, String cityName) {
            this.pincode = pincode;
            this.stateName = stateName;
            this.districtName = districtName;
            this.cityName = cityName;
        }

        public String getPincode() { return pincode; }
        public String getStateName() { return stateName; }
        public String getDistrictName() { return districtName; }
        public String getCityName() { return cityName; }
    }

    public static class PostOfficeResponse {
        @com.fasterxml.jackson.annotation.JsonProperty("Message")
        private String message;
        @com.fasterxml.jackson.annotation.JsonProperty("Status")
        private String status;
        @com.fasterxml.jackson.annotation.JsonProperty("PostOffice")
        private List<PostOfficeDetail> postOffice;

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public List<PostOfficeDetail> getPostOffice() { return postOffice; }
        public void setPostOffice(List<PostOfficeDetail> postOffice) { this.postOffice = postOffice; }
    }

    public static class PostOfficeDetail {
        @com.fasterxml.jackson.annotation.JsonProperty("Name")
        private String name;
        @com.fasterxml.jackson.annotation.JsonProperty("State")
        private String state;
        @com.fasterxml.jackson.annotation.JsonProperty("District")
        private String district;
        @com.fasterxml.jackson.annotation.JsonProperty("Division")
        private String division;
        @com.fasterxml.jackson.annotation.JsonProperty("Circle")
        private String circle;
        @com.fasterxml.jackson.annotation.JsonProperty("Pincode")
        private String pincode;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getState() { return state; }
        public void setState(String state) { this.state = state; }
        public String getDistrict() { return district; }
        public void setDistrict(String district) { this.district = district; }
        public String getDivision() { return division; }
        public void setDivision(String division) { this.division = division; }
        public String getCircle() { return circle; }
        public void setCircle(String circle) { this.circle = circle; }
        public String getPincode() { return pincode; }
        public void setPincode(String pincode) { this.pincode = pincode; }
    }

    private DeliveryResolution fromRule(PincodeDeliveryRule rule, BigDecimal subtotal, boolean codRequested, SiteSettings settings) {
        if (!rule.isServiceable()) {
            return DeliveryResolution.unserviceable("PINCODE", "Delivery is not available for this pincode")
                    .withRule(rule);
        }
        if (codRequested && !rule.isCodAvailable()) {
            return DeliveryResolution.unserviceable("PINCODE", "Cash on delivery is not available for this pincode")
                    .withRule(rule);
        }
        if (!codRequested && !rule.isPrepaidAvailable()) {
            return DeliveryResolution.unserviceable("PINCODE", "Prepaid delivery is not available for this pincode")
                    .withRule(rule);
        }

        BigDecimal threshold = rule.getFreeDeliveryThreshold() != null
                ? rule.getFreeDeliveryThreshold()
                : settings == null ? null : settings.getFreeDeliveryThreshold();
        boolean freeApplied = threshold != null && threshold.compareTo(BigDecimal.ZERO) > 0 && subtotal.compareTo(threshold) >= 0;
        BigDecimal charge = freeApplied ? BigDecimal.ZERO : defaultDecimal(rule.getDeliveryCharge());
        return DeliveryResolution.serviceable(
                        rule.isCodAvailable(),
                        rule.isPrepaidAvailable(),
                        charge.max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP),
                        threshold,
                        freeApplied,
                        rule.getMinDeliveryDays(),
                        rule.getMaxDeliveryDays(),
                        "PINCODE",
                        "Delivery available"
                )
                .withRule(rule);
    }

    private DeliveryResolution fromStateFallback(SiteSettings settings, BigDecimal subtotal, String deliveryState) {
        BigDecimal threshold = settings.getFreeDeliveryThreshold();
        boolean freeApplied = threshold != null && threshold.compareTo(BigDecimal.ZERO) > 0 && subtotal.compareTo(threshold) >= 0;
        BigDecimal charge = freeApplied
                ? BigDecimal.ZERO
                : resolveStateDeliveryCharge(settings.getStateDeliveryCharges(), deliveryState, defaultDecimal(settings.getStandardDeliveryCharge()));
        int[] days = resolveStateDeliveryDays(settings.getStateDeliveryWindows(), deliveryState, settings.getEstimatedDeliveryDays() == null ? 5 : settings.getEstimatedDeliveryDays());
        return DeliveryResolution.serviceable(
                true,
                true,
                charge.max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP),
                threshold,
                freeApplied,
                days[0],
                days[1],
                "STATE_FALLBACK",
                "Delivery available based on state coverage"
        );
    }

    private PincodeDeliveryRule pickBestRule(List<PincodeDeliveryRule> candidates, Long storeId) {
        if (candidates.isEmpty()) {
            return null;
        }
        if (storeId != null) {
            for (PincodeDeliveryRule candidate : candidates) {
                if (candidate.getStore() != null && storeId.equals(candidate.getStore().getId())) {
                    return candidate;
                }
            }
        }
        for (PincodeDeliveryRule candidate : candidates) {
            if (candidate.getStore() == null) {
                return candidate;
            }
        }
        return candidates.get(0);
    }

    private BigDecimal resolveStateDeliveryCharge(String rules, String deliveryState, BigDecimal fallback) {
        String targetState = normalizeState(deliveryState);
        if (rules != null && !rules.isBlank() && targetState != null) {
            String[] entries = rules.split("\\r?\\n|;");
            for (String entry : entries) {
                String[] parts = entry.split("=", 2);
                if (parts.length != 2 || !normalizeState(parts[0]).equals(targetState)) {
                    continue;
                }
                try {
                    return new BigDecimal(parts[1].trim());
                } catch (NumberFormatException ignored) {
                    return fallback;
                }
            }
        }
        return fallback;
    }

    private int[] resolveStateDeliveryDays(String rules, String deliveryState, int fallbackDays) {
        String targetState = normalizeState(deliveryState);
        if (rules != null && !rules.isBlank() && targetState != null) {
            String[] entries = rules.split("\\r?\\n|;");
            for (String entry : entries) {
                String[] parts = entry.split("=", 2);
                if (parts.length != 2 || !normalizeState(parts[0]).equals(targetState)) {
                    continue;
                }
                return parseDayRange(parts[1].trim(), fallbackDays);
            }
        }
        return new int[] { fallbackDays, fallbackDays };
    }

    private int[] parseDayRange(String raw, int fallback) {
        if (raw == null || raw.isBlank()) {
            return new int[] { fallback, fallback };
        }
        String normalized = raw.trim().replace("days", "").replace("day", "").trim();
        if (normalized.contains("-")) {
            String[] parts = normalized.split("-", 2);
            int min = sanitizeDayValue(parseInteger(parts[0]), fallback);
            int max = sanitizeDayValue(parseInteger(parts[1]), min);
            return new int[] { min, Math.max(min, max) };
        }
        int exact = sanitizeDayValue(parseInteger(normalized), fallback);
        return new int[] { exact, exact };
    }

    private Map<String, Integer> headerIndex(String headerLine) {
        Map<String, Integer> index = new TreeMap<>();
        List<String> cells = parseCsvLine(headerLine);
        for (int i = 0; i < cells.size(); i++) {
            index.put(cells.get(i).replaceAll("[^A-Za-z0-9]", "").toLowerCase(Locale.ROOT), i);
        }
        return index;
    }

    private String cell(List<String> cells, Map<String, Integer> headerIndex, String key) {
        Integer index = headerIndex.get(key.toLowerCase(Locale.ROOT));
        if (index == null || index < 0 || index >= cells.size()) {
            return null;
        }
        return trimToNull(cells.get(index));
    }

    private List<String> parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    quoted = !quoted;
                }
                continue;
            }
            if (ch == ',' && !quoted) {
                values.add(current.toString());
                current.setLength(0);
                continue;
            }
            current.append(ch);
        }
        values.add(current.toString());
        return values;
    }

    private PincodeDeliveryRuleResponse toResponse(PincodeDeliveryRule rule) {
        return PincodeDeliveryRuleResponse.builder()
                .id(rule.getId())
                .pincode(rule.getPincode())
                .countryCode(rule.getCountryCode())
                .stateName(rule.getStateName())
                .cityName(rule.getCityName())
                .zoneName(rule.getZoneName())
                .serviceable(rule.isServiceable())
                .codAvailable(rule.isCodAvailable())
                .prepaidAvailable(rule.isPrepaidAvailable())
                .deliveryCharge(rule.getDeliveryCharge())
                .freeDeliveryThreshold(rule.getFreeDeliveryThreshold())
                .minDeliveryDays(rule.getMinDeliveryDays())
                .maxDeliveryDays(rule.getMaxDeliveryDays())
                .storeId(rule.getStore() == null ? null : rule.getStore().getId())
                .storeName(rule.getStore() == null ? null : rule.getStore().getName())
                .priority(rule.getPriority())
                .active(rule.isActive())
                .notes(rule.getNotes())
                .createdAt(rule.getCreatedAt())
                .updatedAt(rule.getUpdatedAt())
                .build();
    }

    private String normalizePincode(String value) {
        String digits = value == null ? "" : value.replaceAll("\\D", "");
        if (digits.length() != 6) {
            throw new BadRequestException("Pincode must be exactly 6 digits");
        }
        return digits;
    }

    private String normalizeCountryCode(String value) {
        String trimmed = trimToNull(value);
        return trimmed == null ? "IN" : trimmed.toUpperCase(Locale.ROOT);
    }

    private BigDecimal nonNegative(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value.max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal defaultDecimal(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private Integer parseInteger(String value) {
        try {
            return value == null || value.isBlank() ? null : Integer.parseInt(value.trim());
        } catch (NumberFormatException exception) {
            throw new BadRequestException("Invalid integer value: " + value);
        }
    }

    private Integer parseInteger(Object value) {
        return value == null ? null : parseInteger(String.valueOf(value));
    }

    private Long parseLong(String value) {
        try {
            return value == null || value.isBlank() ? null : Long.parseLong(value.trim());
        } catch (NumberFormatException exception) {
            throw new BadRequestException("Invalid store id: " + value);
        }
    }

    private BigDecimal parseDecimal(String value) {
        try {
            return value == null || value.isBlank() ? null : new BigDecimal(value.trim());
        } catch (NumberFormatException exception) {
            throw new BadRequestException("Invalid decimal value: " + value);
        }
    }

    private Boolean parseBoolean(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return normalized.equals("true") || normalized.equals("1") || normalized.equals("yes") || normalized.equals("y");
    }

    private int sanitizeDayValue(Integer value, int fallback) {
        int candidate = value == null ? fallback : value;
        return Math.max(1, candidate);
    }

    private String normalizeState(String value) {
        String trimmed = trimToNull(value);
        return trimmed == null ? null : trimmed.toUpperCase(Locale.ROOT);
    }

    private String estimatedLabel(Integer minDays, Integer maxDays) {
        int min = sanitizeDayValue(minDays, 1);
        int max = sanitizeDayValue(maxDays, min);
        if (min == max) {
            return min + " business day" + (min == 1 ? "" : "s");
        }
        return min + "-" + max + " business days";
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        String escaped = value.replace("\"", "\"\"");
        if (escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n") || escaped.contains("\r")) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }

    // Zones CRUD
    public List<PincodeZone> getZones() {
        return pincodeZoneRepository.findAllByOrderByPriorityAscIdAsc();
    }

    @Transactional
    public PincodeZone saveZone(PincodeZone zone) {
        return pincodeZoneRepository.save(zone);
    }

    @Transactional
    public void deleteZone(Long id) {
        pincodeZoneRepository.deleteById(id);
    }

    // Blacklist CRUD
    public List<PincodeBlacklist> getBlacklist() {
        return pincodeBlacklistRepository.findAllByOrderByPincodeAsc();
    }

    @Transactional
    public PincodeBlacklist saveBlacklist(PincodeBlacklist entry) {
        entry.setPincode(normalizePincode(entry.getPincode()));
        return pincodeBlacklistRepository.save(entry);
    }

    @Transactional
    public void deleteBlacklist(Long id) {
        pincodeBlacklistRepository.deleteById(id);
    }

    // Holidays CRUD
    public List<Holiday> getHolidays() {
        return holidayRepository.findAllByOrderByHolidayDateAsc();
    }

    @Transactional
    public Holiday saveHoliday(Holiday holiday) {
        return holidayRepository.save(holiday);
    }

    @Transactional
    public void deleteHoliday(Long id) {
        holidayRepository.deleteById(id);
    }

    // Logs Query
    public List<PincodeLookupLog> getLookupLogs() {
        return pincodeLookupLogRepository.findTop100ByOrderByCreatedAtDesc();
    }

    public static final class DeliveryResolution {
        private final boolean serviceable;
        private final boolean codAvailable;
        private final boolean prepaidAvailable;
        private final BigDecimal deliveryCharge;
        private final BigDecimal freeDeliveryThreshold;
        private final boolean freeDeliveryApplied;
        private final Integer minDeliveryDays;
        private final Integer maxDeliveryDays;
        private final String ruleSource;
        private final String message;
        private Long ruleId;
        private Long storeId;
        private String storeName;
        private PincodeLocation location;

        public PincodeLocation getLocation() {
            return location;
        }

        public void setLocation(PincodeLocation location) {
            this.location = location;
        }

        private DeliveryResolution(boolean serviceable, boolean codAvailable, boolean prepaidAvailable, BigDecimal deliveryCharge,
                                   BigDecimal freeDeliveryThreshold, boolean freeDeliveryApplied, Integer minDeliveryDays,
                                   Integer maxDeliveryDays, String ruleSource, String message) {
            this.serviceable = serviceable;
            this.codAvailable = codAvailable;
            this.prepaidAvailable = prepaidAvailable;
            this.deliveryCharge = deliveryCharge;
            this.freeDeliveryThreshold = freeDeliveryThreshold;
            this.freeDeliveryApplied = freeDeliveryApplied;
            this.minDeliveryDays = minDeliveryDays;
            this.maxDeliveryDays = maxDeliveryDays;
            this.ruleSource = ruleSource;
            this.message = message;
        }

        public static DeliveryResolution serviceable(boolean codAvailable, boolean prepaidAvailable, BigDecimal deliveryCharge, BigDecimal freeDeliveryThreshold, boolean freeDeliveryApplied,
                                                     Integer minDeliveryDays, Integer maxDeliveryDays, String ruleSource, String message) {
            return new DeliveryResolution(true, codAvailable, prepaidAvailable, deliveryCharge, freeDeliveryThreshold, freeDeliveryApplied, minDeliveryDays, maxDeliveryDays, ruleSource, message);
        }

        public static DeliveryResolution unserviceable(String ruleSource, String message) {
            return new DeliveryResolution(false, false, false, BigDecimal.ZERO, null, false, null, null, ruleSource, message);
        }

        public DeliveryResolution withRule(PincodeDeliveryRule rule) {
            this.ruleId = rule.getId();
            this.storeId = rule.getStore() == null ? null : rule.getStore().getId();
            this.storeName = rule.getStore() == null ? null : rule.getStore().getName();
            return this;
        }

        public boolean isServiceable() {
            return serviceable;
        }

        public boolean isCodAvailable() {
            return serviceable && codAvailable;
        }

        public boolean isPrepaidAvailable() {
            return serviceable && prepaidAvailable;
        }

        public BigDecimal getDeliveryCharge() {
            return deliveryCharge;
        }

        public BigDecimal getFreeDeliveryThreshold() {
            return freeDeliveryThreshold;
        }

        public boolean isFreeDeliveryApplied() {
            return freeDeliveryApplied;
        }

        public Integer getMinDeliveryDays() {
            return minDeliveryDays;
        }

        public Integer getMaxDeliveryDays() {
            return maxDeliveryDays;
        }

        public String getRuleSource() {
            return ruleSource;
        }

        public String getMessage() {
            return message;
        }

        public Long getRuleId() {
            return ruleId;
        }

        public Long getStoreId() {
            return storeId;
        }

        public String getStoreName() {
            return storeName;
        }
    }
}
