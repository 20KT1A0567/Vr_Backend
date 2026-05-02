package com.vrtechnologies.vrtech.repository;

import com.vrtechnologies.vrtech.entity.OrderItem;
import com.vrtechnologies.vrtech.entity.enums.OrderStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    @Query("""
            select oi.product.id as productId, sum(oi.quantity) as soldQuantity
            from OrderItem oi
            join oi.order o
            where o.status not in :excludedStatuses
            group by oi.product.id
            order by sum(oi.quantity) desc
            """)
    List<ProductSalesProjection> findTopSellingProducts(
            @Param("excludedStatuses") Collection<OrderStatus> excludedStatuses,
            Pageable pageable
    );

    interface ProductSalesProjection {
        Long getProductId();
        Long getSoldQuantity();
    }
}
