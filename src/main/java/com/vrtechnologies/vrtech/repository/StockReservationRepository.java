package com.vrtechnologies.vrtech.repository;

import com.vrtechnologies.vrtech.entity.StockReservation;
import com.vrtechnologies.vrtech.entity.enums.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface StockReservationRepository extends JpaRepository<StockReservation, Long> {
    List<StockReservation> findByOrderId(Long orderId);
    List<StockReservation> findByOrderIdAndStatus(Long orderId, ReservationStatus status);
    List<StockReservation> findByStatusAndExpiresAtBefore(ReservationStatus status, LocalDateTime dateTime);
    List<StockReservation> findByStatusAndExpiresAtAfter(ReservationStatus status, LocalDateTime dateTime);

    @Query("SELECT COALESCE(SUM(sr.quantity), 0) FROM StockReservation sr WHERE sr.product.id = :productId AND sr.status = 'PENDING' AND sr.expiresAt > :now")
    int getReservedQuantityForProductGlobal(@Param("productId") Long productId, @Param("now") LocalDateTime now);

    @Query("SELECT COALESCE(SUM(sr.quantity), 0) FROM StockReservation sr WHERE sr.product.id = :productId AND sr.store.id = :storeId AND sr.status = 'PENDING' AND sr.expiresAt > :now")
    int getReservedQuantityForProductStore(@Param("productId") Long productId, @Param("storeId") Long storeId, @Param("now") LocalDateTime now);

    @Query("SELECT COALESCE(SUM(sr.quantity), 0) FROM StockReservation sr WHERE sr.productVariant.id = :variantId AND sr.status = 'PENDING' AND sr.expiresAt > :now")
    int getReservedQuantityForVariant(@Param("variantId") Long variantId, @Param("now") LocalDateTime now);
}
