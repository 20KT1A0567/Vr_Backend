package com.vrtechnologies.vrtech.service;

import com.vrtechnologies.vrtech.dto.response.LowStockPredictionResponse;
import com.vrtechnologies.vrtech.entity.Product;
import com.vrtechnologies.vrtech.entity.enums.OrderStatus;
import com.vrtechnologies.vrtech.entity.enums.ProductStatus;
import com.vrtechnologies.vrtech.repository.OrderItemRepository;
import com.vrtechnologies.vrtech.repository.OrderItemRepository.ProductSalesProjection;
import com.vrtechnologies.vrtech.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class InventoryPredictionService {

    private final ProductRepository productRepository;
    private final OrderItemRepository orderItemRepository;

    public InventoryPredictionService(
            ProductRepository productRepository,
            OrderItemRepository orderItemRepository
    ) {
        this.productRepository = productRepository;
        this.orderItemRepository = orderItemRepository;
    }

    @Transactional(readOnly = true)
    public List<LowStockPredictionResponse> getPredictions() {
        // Exclude cancelled and refunded orders
        Collection<OrderStatus> excludedStatuses = List.of(OrderStatus.CANCELLED, OrderStatus.REFUNDED);

        // Fetch sales aggregations
        LocalDateTime now = LocalDateTime.now();
        List<ProductSalesProjection> sales30d = orderItemRepository.sumQuantitySoldSinceGroupedByProduct(now.minusDays(30), excludedStatuses);
        List<ProductSalesProjection> sales7d = orderItemRepository.sumQuantitySoldSinceGroupedByProduct(now.minusDays(7), excludedStatuses);
        List<ProductSalesProjection> sales3d = orderItemRepository.sumQuantitySoldSinceGroupedByProduct(now.minusDays(3), excludedStatuses);

        // Map product ID -> sales quantity
        Map<Long, Double> salesMap30d = sales30d.stream().collect(Collectors.toMap(ProductSalesProjection::getProductId, p -> (double) p.getSoldQuantity()));
        Map<Long, Double> salesMap7d = sales7d.stream().collect(Collectors.toMap(ProductSalesProjection::getProductId, p -> (double) p.getSoldQuantity()));
        Map<Long, Double> salesMap3d = sales3d.stream().collect(Collectors.toMap(ProductSalesProjection::getProductId, p -> (double) p.getSoldQuantity()));

        // Fetch all active products
        List<Product> products = productRepository.findAll().stream()
                .filter(p -> p.getProductStatus() == null || p.getEffectiveProductStatus() == ProductStatus.ACTIVE)
                .toList();

        List<LowStockPredictionResponse> predictions = new ArrayList<>();

        for (Product product : products) {
            Long pid = product.getId();
            double qty30d = salesMap30d.getOrDefault(pid, 0.0);
            double qty7d = salesMap7d.getOrDefault(pid, 0.0);
            double qty3d = salesMap3d.getOrDefault(pid, 0.0);

            double velocity30d = qty30d / 30.0;
            double velocity7d = qty7d / 7.0;
            double velocity3d = qty3d / 3.0;

            // Spike factor: compare short-term (3d) vs long-term (30d)
            double spikeFactor = 1.0;
            if (velocity30d > 0) {
                spikeFactor = velocity3d / velocity30d;
            } else if (velocity3d > 0) {
                spikeFactor = 3.0; // Assume 3x spike if new/sudden demand
            }

            // Weighted average daily velocity
            double averageDailyVelocity = (0.5 * velocity7d) + (0.3 * velocity3d) + (0.2 * velocity30d);
            
            // Limit minimum daily velocity to 0 to avoid negative values
            if (averageDailyVelocity < 0.0) {
                averageDailyVelocity = 0.0;
            }

            int currentStock = product.getStockQuantity();
            int threshold = product.getResolvedLowStockThreshold();
            int leadTime = product.getResolvedLeadTimeDays();

            Integer daysToStockout = null;
            if (averageDailyVelocity > 0.0) {
                daysToStockout = (int) Math.ceil(currentStock / averageDailyVelocity);
            }

            int recommendedReorder = 0;
            // Recommend reorder if current stock is below threshold or stockout is imminent (<= 14 days)
            if (currentStock <= threshold || (daysToStockout != null && daysToStockout <= 14)) {
                // target stock covers lead time + safety stock buffer
                double targetStock = (averageDailyVelocity * (leadTime + 3)) + (threshold * 1.5);
                recommendedReorder = Math.max(5, (int) Math.ceil(targetStock - currentStock));
            }

            // Round values for DTO output formatting
            double roundedVelocity30d = Math.round(velocity30d * 100.0) / 100.0;
            double roundedVelocity7d = Math.round(velocity7d * 100.0) / 100.0;
            double roundedVelocity3d = Math.round(velocity3d * 100.0) / 100.0;
            double roundedAverageVelocity = Math.round(averageDailyVelocity * 100.0) / 100.0;
            double roundedSpikeFactor = Math.round(spikeFactor * 10.0) / 10.0;

            String warningMessage = null;
            if (daysToStockout != null && daysToStockout <= 14) {
                if (spikeFactor >= 1.5 && velocity3d > 0.1) {
                    warningMessage = String.format("⚠ %s is selling %.1fx faster than usual. Expected stockout: %d days. Recommended reorder: %d units.",
                            product.getTitle(), roundedSpikeFactor, daysToStockout, recommendedReorder);
                } else {
                    warningMessage = String.format("⚠ Low stock alert for %s. Expected stockout: %d days. Recommended reorder: %d units.",
                            product.getTitle(), daysToStockout, recommendedReorder);
                }
            } else if (currentStock <= threshold) {
                warningMessage = String.format("⚠ Low stock threshold breached for %s. Current stock: %d. Recommended reorder: %d units.",
                        product.getTitle(), currentStock, recommendedReorder);
            }

            predictions.add(LowStockPredictionResponse.builder()
                    .productId(pid)
                    .productTitle(product.getTitle())
                    .productSku(product.getSku())
                    .currentStock(currentStock)
                    .lowStockThreshold(threshold)
                    .leadTimeDays(leadTime)
                    .velocity30d(roundedVelocity30d)
                    .velocity7d(roundedVelocity7d)
                    .velocity3d(roundedVelocity3d)
                    .averageDailyVelocity(roundedAverageVelocity)
                    .spikeFactor(roundedSpikeFactor)
                    .daysToStockout(daysToStockout)
                    .recommendedReorder(recommendedReorder)
                    .warningMessage(warningMessage)
                    .build());
        }

        return predictions;
    }
}
