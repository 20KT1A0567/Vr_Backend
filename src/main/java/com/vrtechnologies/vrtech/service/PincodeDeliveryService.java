package com.vrtechnologies.vrtech.service;

import com.vrtechnologies.vrtech.dto.request.PincodeDeliveryRuleRequest;
import com.vrtechnologies.vrtech.dto.response.PincodeDeliveryCheckResponse;
import com.vrtechnologies.vrtech.dto.response.PincodeDeliveryRuleResponse;
import com.vrtechnologies.vrtech.dto.response.ProductImportResponse;
import com.vrtechnologies.vrtech.entity.PincodeDeliveryRule;
import com.vrtechnologies.vrtech.entity.SiteSettings;
import com.vrtechnologies.vrtech.entity.Store;
import com.vrtechnologies.vrtech.exception.BadRequestException;
import com.vrtechnologies.vrtech.exception.ResourceNotFoundException;
import com.vrtechnologies.vrtech.repository.PincodeDeliveryRuleRepository;
import com.vrtechnologies.vrtech.repository.StoreRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
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

    public PincodeDeliveryService(PincodeDeliveryRuleRepository pincodeDeliveryRuleRepository, StoreRepository storeRepository) {
        this.pincodeDeliveryRuleRepository = pincodeDeliveryRuleRepository;
        this.storeRepository = storeRepository;
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
        }
        if (normalizedPincode != null) {
            List<PincodeDeliveryRule> candidates = pincodeDeliveryRuleRepository.findByPincodeAndActiveTrueOrderByPriorityAscIdAsc(normalizedPincode);
            PincodeDeliveryRule best = pickBestRule(candidates, storeId);
            if (best != null) {
                return fromRule(best, safeSubtotal, codRequested, settings);
            }
        }

        if (settings != null && deliveryState != null && !deliveryState.isBlank()) {
            return fromStateFallback(settings, safeSubtotal, deliveryState);
        }

        return DeliveryResolution.unserviceable("PINCODE", "This pincode is not serviceable yet");
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
