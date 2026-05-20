package com.vrtechnologies.vrtech.service;

import com.vrtechnologies.vrtech.dto.response.ReturnRequestResponse;
import com.vrtechnologies.vrtech.dto.request.ReturnPickupRequest;
import com.vrtechnologies.vrtech.entity.CustomerOrder;
import com.vrtechnologies.vrtech.entity.ReturnRequest;
import com.vrtechnologies.vrtech.entity.User;
import com.vrtechnologies.vrtech.entity.enums.OrderStatus;
import com.vrtechnologies.vrtech.entity.enums.OrderTimelineEventType;
import com.vrtechnologies.vrtech.entity.enums.PaymentStatus;
import com.vrtechnologies.vrtech.entity.enums.ReturnRequestStatus;
import com.vrtechnologies.vrtech.exception.BadRequestException;
import com.vrtechnologies.vrtech.exception.ResourceNotFoundException;
import com.vrtechnologies.vrtech.repository.CustomerOrderRepository;
import com.vrtechnologies.vrtech.repository.ReturnRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReturnRequestService {

    private final ReturnRequestRepository returnRequestRepository;
    private final CustomerOrderRepository customerOrderRepository;
    private final UserContextService userContextService;
    private final OrderTimelineService orderTimelineService;
    private final NotificationService notificationService;

    public ReturnRequestService(
            ReturnRequestRepository returnRequestRepository,
            CustomerOrderRepository customerOrderRepository,
            UserContextService userContextService,
            OrderTimelineService orderTimelineService,
            NotificationService notificationService
    ) {
        this.returnRequestRepository = returnRequestRepository;
        this.customerOrderRepository = customerOrderRepository;
        this.userContextService = userContextService;
        this.orderTimelineService = orderTimelineService;
        this.notificationService = notificationService;
    }

    @Transactional
    public ReturnRequestResponse requestReturn(Long orderId, String reason) {
        User user = userContextService.getCurrentUser();
        CustomerOrder order = customerOrderRepository.findById(orderId)
                .filter(item -> item.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        if (order.getStatus() != OrderStatus.DELIVERED) {
            throw new BadRequestException("Return requests are allowed only after delivery");
        }
        if (returnRequestRepository.findFirstByOrderIdOrderByCreatedAtDescIdDesc(orderId)
                .filter(item -> item.getStatus() != ReturnRequestStatus.REJECTED && item.getStatus() != ReturnRequestStatus.CLOSED)
                .isPresent()) {
            throw new BadRequestException("A return request already exists for this order");
        }

        ReturnRequest request = new ReturnRequest();
        request.setOrder(order);
        request.setUser(user);
        request.setReason(reason == null || reason.isBlank() ? "No reason provided" : reason.trim());
        ReturnRequest saved = returnRequestRepository.save(request);

        order.setStatus(OrderStatus.RETURN_REQUESTED);
        order.setReturnRequestedAt(LocalDateTime.now());
        order.setReturnReason(request.getReason());
        customerOrderRepository.save(order);
        orderTimelineService.record(order, OrderTimelineEventType.RETURN_REQUESTED, "Return requested", "Customer requested a return.", user, "WEBSITE");
        notificationService.logOrderEvent("RETURN_REQUESTED", order, "Return requested", "Your return request has been received.");
        return toResponse(saved);
    }

    public List<ReturnRequestResponse> list(ReturnRequestStatus status) {
        List<ReturnRequest> items = status == null
                ? returnRequestRepository.findAllByOrderByCreatedAtDescIdDesc()
                : returnRequestRepository.findByStatusOrderByCreatedAtDescIdDesc(status);
        return items.stream().map(this::toResponse).toList();
    }

    @Transactional
    public ReturnRequestResponse approve(User admin, Long id, String note) {
        ReturnRequest request = require(id);
        request.setStatus(ReturnRequestStatus.APPROVED);
        request.setAdminNote(normalize(note));
        request.setResolvedAt(LocalDateTime.now());
        request.setResolvedBy(admin.getId());
        request.getOrder().setReturnResolutionNote(normalize(note));
        orderTimelineService.record(request.getOrder(), OrderTimelineEventType.RETURN_REQUESTED, "Return approved", "Admin approved the return request.", admin, "ADMIN");
        notificationService.logOrderEvent("RETURN_APPROVED", request.getOrder(), "Return approved", "Your return request has been approved.");
        return toResponse(returnRequestRepository.save(request));
    }

    @Transactional
    public ReturnRequestResponse reject(User admin, Long id, String note) {
        ReturnRequest request = require(id);
        request.setStatus(ReturnRequestStatus.REJECTED);
        request.setAdminNote(normalize(note));
        request.setResolvedAt(LocalDateTime.now());
        request.setResolvedBy(admin.getId());
        request.getOrder().setStatus(OrderStatus.DELIVERED);
        request.getOrder().setReturnResolutionNote(normalize(note));
        orderTimelineService.record(request.getOrder(), OrderTimelineEventType.DELIVERED, "Return rejected", "Admin rejected the return request.", admin, "ADMIN");
        notificationService.logOrderEvent("RETURN_REJECTED", request.getOrder(), "Return rejected", "Your return request has been rejected.");
        return toResponse(returnRequestRepository.save(request));
    }

    @Transactional
    public ReturnRequestResponse markRefunded(User admin, Long id, String note) {
        ReturnRequest request = require(id);
        request.setStatus(ReturnRequestStatus.REFUNDED);
        request.setAdminNote(normalize(note));
        request.setResolvedAt(LocalDateTime.now());
        request.setResolvedBy(admin.getId());
        request.getOrder().setStatus(OrderStatus.REFUNDED);
        request.getOrder().setPaymentStatus(PaymentStatus.REFUNDED);
        request.getOrder().setReturnResolutionNote(normalize(note));
        orderTimelineService.record(request.getOrder(), OrderTimelineEventType.REFUNDED, "Refund processed", "Admin marked the return refund as processed.", admin, "ADMIN");
        notificationService.logOrderEvent("RETURN_REFUNDED", request.getOrder(), "Refund processed", "Your refund has been marked as processed.");
        return toResponse(returnRequestRepository.save(request));
    }

    @Transactional
    public ReturnRequestResponse schedulePickup(User admin, Long id, ReturnPickupRequest pickup) {
        ReturnRequest request = require(id);
        request.setStatus(ReturnRequestStatus.PICKUP_SCHEDULED);
        request.setPickupScheduledAt(pickup != null ? pickup.getPickupScheduledAt() : null);
        request.setPickupAgent(normalize(pickup != null ? pickup.getPickupAgent() : null));
        request.setPickupTrackingNumber(normalize(pickup != null ? pickup.getPickupTrackingNumber() : null));
        request.setAdminNote(normalize(pickup != null ? pickup.getNote() : null));
        request.setResolvedBy(admin.getId());
        orderTimelineService.record(request.getOrder(), OrderTimelineEventType.RETURN_REQUESTED, "Return pickup scheduled", "Admin scheduled the return pickup.", admin, "ADMIN");
        notificationService.logOrderEvent("RETURN_PICKUP_SCHEDULED", request.getOrder(), "Return pickup scheduled", "Your return pickup has been scheduled.");
        return toResponse(returnRequestRepository.save(request));
    }

    @Transactional
    public ReturnRequestResponse markPickedUp(User admin, Long id, String note) {
        ReturnRequest request = require(id);
        request.setStatus(ReturnRequestStatus.PICKED_UP);
        request.setPickedUpAt(LocalDateTime.now());
        request.setAdminNote(normalize(note));
        request.setResolvedBy(admin.getId());
        orderTimelineService.record(request.getOrder(), OrderTimelineEventType.RETURN_REQUESTED, "Return picked up", "The return item was picked up.", admin, "ADMIN");
        notificationService.logOrderEvent("RETURN_PICKED_UP", request.getOrder(), "Return picked up", "Your return item has been picked up.");
        return toResponse(returnRequestRepository.save(request));
    }

    @Transactional
    public ReturnRequestResponse inspect(User admin, Long id, String note) {
        ReturnRequest request = require(id);
        request.setStatus(ReturnRequestStatus.INSPECTED);
        request.setInspectedAt(LocalDateTime.now());
        request.setInspectionNote(normalize(note));
        request.setResolvedBy(admin.getId());
        orderTimelineService.record(request.getOrder(), OrderTimelineEventType.RETURN_REQUESTED, "Return inspected", "Admin completed return inspection.", admin, "ADMIN");
        notificationService.logOrderEvent("RETURN_INSPECTED", request.getOrder(), "Return inspected", "Your return has been inspected and is awaiting refund processing.");
        return toResponse(returnRequestRepository.save(request));
    }

    private ReturnRequest require(Long id) {
        return returnRequestRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Return request not found"));
    }

    private ReturnRequestResponse toResponse(ReturnRequest request) {
        return ReturnRequestResponse.builder()
                .id(request.getId())
                .orderId(request.getOrder().getId())
                .orderNumber(request.getOrder().getOrderNumber())
                .userId(request.getUser().getId())
                .customerName(request.getOrder().getContactName())
                .reason(request.getReason())
                .status(request.getStatus())
                .adminNote(request.getAdminNote())
                .resolvedAt(request.getResolvedAt())
                .resolvedBy(request.getResolvedBy())
                .pickupScheduledAt(request.getPickupScheduledAt())
                .pickedUpAt(request.getPickedUpAt())
                .inspectedAt(request.getInspectedAt())
                .pickupAgent(request.getPickupAgent())
                .pickupTrackingNumber(request.getPickupTrackingNumber())
                .inspectionNote(request.getInspectionNote())
                .createdAt(request.getCreatedAt())
                .updatedAt(request.getUpdatedAt())
                .build();
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
