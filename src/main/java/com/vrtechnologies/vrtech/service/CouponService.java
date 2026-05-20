package com.vrtechnologies.vrtech.service;

import com.vrtechnologies.vrtech.dto.response.CouponValidationResponse;
import com.vrtechnologies.vrtech.entity.Coupon;
import com.vrtechnologies.vrtech.entity.enums.CouponStatus;
import com.vrtechnologies.vrtech.exception.BadRequestException;
import com.vrtechnologies.vrtech.repository.CouponRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Locale;

@Service
public class CouponService {

    private final CouponRepository couponRepository;

    public CouponService(CouponRepository couponRepository) {
        this.couponRepository = couponRepository;
    }

    public CouponValidationResponse validate(String code, BigDecimal subtotal) {
        BigDecimal safeSubtotal = subtotal == null ? BigDecimal.ZERO : subtotal;
        String normalized = normalizeCode(code);
        Coupon coupon = couponRepository.findByCodeIgnoreCase(normalized).orElse(null);
        if (coupon == null) {
            return invalid(normalized, safeSubtotal, "Coupon not found");
        }
        String invalidReason = invalidReason(coupon, safeSubtotal);
        if (invalidReason != null) {
            return invalid(normalized, safeSubtotal, invalidReason);
        }
        BigDecimal discount = calculateDiscount(coupon, safeSubtotal);
        return CouponValidationResponse.builder()
                .code(coupon.getCode())
                .valid(true)
                .message("Coupon applied")
                .subtotal(safeSubtotal)
                .discountAmount(discount)
                .finalAmount(safeSubtotal.subtract(discount).max(BigDecimal.ZERO))
                .minOrder(coupon.getMinOrder())
                .expiryDate(coupon.getExpiryDate())
                .remainingUses(remainingUses(coupon))
                .build();
    }

    @Transactional
    public CouponValidationResponse apply(String code, BigDecimal subtotal) {
        CouponValidationResponse validation = validate(code, subtotal);
        if (!validation.isValid()) {
            throw new BadRequestException(validation.getMessage());
        }
        Coupon coupon = couponRepository.findByCodeIgnoreCase(validation.getCode())
                .orElseThrow(() -> new BadRequestException("Coupon not found"));
        coupon.setUsageCount((coupon.getUsageCount() == null ? 0 : coupon.getUsageCount()) + 1);
        coupon.setTotalDiscountGiven(safe(coupon.getTotalDiscountGiven()).add(validation.getDiscountAmount()));
        coupon.setTotalRevenueGenerated(safe(coupon.getTotalRevenueGenerated()).add(validation.getFinalAmount()));
        couponRepository.save(coupon);
        return validation;
    }

    private String invalidReason(Coupon coupon, BigDecimal subtotal) {
        if (coupon.getStatus() != CouponStatus.ACTIVE) {
            return "Coupon is not active";
        }
        if (coupon.getExpiryDate() != null && coupon.getExpiryDate().isBefore(LocalDate.now())) {
            return "Coupon has expired";
        }
        if (coupon.getUsageLimit() != null && coupon.getUsageLimit() > 0
                && (coupon.getUsageCount() == null ? 0 : coupon.getUsageCount()) >= coupon.getUsageLimit()) {
            return "Coupon usage limit reached";
        }
        if (subtotal.compareTo(safe(coupon.getMinOrder())) < 0) {
            return "Minimum order amount is " + coupon.getMinOrder();
        }
        return null;
    }

    private BigDecimal calculateDiscount(Coupon coupon, BigDecimal subtotal) {
        BigDecimal discount = safe(coupon.getDiscount());
        return discount.min(subtotal).max(BigDecimal.ZERO);
    }

    private CouponValidationResponse invalid(String code, BigDecimal subtotal, String message) {
        return CouponValidationResponse.builder()
                .code(code)
                .valid(false)
                .message(message)
                .subtotal(subtotal)
                .discountAmount(BigDecimal.ZERO)
                .finalAmount(subtotal)
                .remainingUses(0)
                .build();
    }

    private Integer remainingUses(Coupon coupon) {
        if (coupon.getUsageLimit() == null || coupon.getUsageLimit() <= 0) {
            return null;
        }
        return Math.max(0, coupon.getUsageLimit() - (coupon.getUsageCount() == null ? 0 : coupon.getUsageCount()));
    }

    private String normalizeCode(String code) {
        return code == null ? "" : code.trim().toUpperCase(Locale.ROOT);
    }

    private BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
