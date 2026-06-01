package com.vrtechnologies.vrtech.scheduler;

import com.vrtechnologies.vrtech.entity.CustomerOrder;
import com.vrtechnologies.vrtech.entity.StockReservation;
import com.vrtechnologies.vrtech.entity.enums.OrderStatus;
import com.vrtechnologies.vrtech.entity.enums.OrderTimelineEventType;
import com.vrtechnologies.vrtech.entity.enums.ReservationStatus;
import com.vrtechnologies.vrtech.repository.CustomerOrderRepository;
import com.vrtechnologies.vrtech.repository.StockReservationRepository;
import com.vrtechnologies.vrtech.service.OrderTimelineService;
import com.vrtechnologies.vrtech.service.SseEmitterService;
import com.vrtechnologies.vrtech.dto.event.SystemEvent;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Component
public class ReservationCleanupScheduler {

    private final StockReservationRepository stockReservationRepository;
    private final CustomerOrderRepository customerOrderRepository;
    private final OrderTimelineService orderTimelineService;
    private final SseEmitterService sseEmitterService;

    public ReservationCleanupScheduler(
            StockReservationRepository stockReservationRepository,
            CustomerOrderRepository customerOrderRepository,
            OrderTimelineService orderTimelineService,
            SseEmitterService sseEmitterService
    ) {
        this.stockReservationRepository = stockReservationRepository;
        this.customerOrderRepository = customerOrderRepository;
        this.orderTimelineService = orderTimelineService;
        this.sseEmitterService = sseEmitterService;
    }

    // Run every 60 seconds to release inventory for unpaid online orders
    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void cleanupExpiredReservations() {
        LocalDateTime now = LocalDateTime.now();
        List<StockReservation> expiredReservations = stockReservationRepository
                .findByStatusAndExpiresAtBefore(ReservationStatus.PENDING, now);

        for (StockReservation reservation : expiredReservations) {
            reservation.setStatus(ReservationStatus.RELEASED);
            stockReservationRepository.save(reservation);

            CustomerOrder order = reservation.getOrder();
            if (order.getStatus() == OrderStatus.PENDING) {
                order.setStatus(OrderStatus.CANCELLED);
                order.setCancellationReason("Checkout time-limit (15 minutes) expired without payment.");
                customerOrderRepository.save(order);

                orderTimelineService.record(
                        order,
                        OrderTimelineEventType.CANCELLED,
                        "Order expired",
                        "Stock hold released back to inventory because payment was not completed within the 15-minute checkout window.",
                        null,
                        "SYSTEM"
                );
            }

            // Broadcast real-time stock mutation
            if (reservation.getProduct() != null) {
                try {
                    SystemEvent event = SystemEvent.builder()
                            .eventType("STOCK_MUTATION")
                            .title("Inventory Mutated")
                            .message("Product inventory state updated")
                            .severity("INFO")
                            .payload(Map.of("productId", reservation.getProduct().getId()))
                            .timestamp(LocalDateTime.now())
                            .build();
                    sseEmitterService.broadcast(event);
                } catch (Exception e) {
                    // suppress
                }
            }
        }
    }
}
