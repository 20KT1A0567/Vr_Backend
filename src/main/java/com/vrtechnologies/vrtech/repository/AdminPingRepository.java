package com.vrtechnologies.vrtech.repository;

import com.vrtechnologies.vrtech.entity.AdminPing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AdminPingRepository extends JpaRepository<AdminPing, Long> {
    
    // Fetch all pings ordered by timestamp ascending (chronological order)
    List<AdminPing> findAllByOrderByPingTimestampAsc();

    // Fetch pings filtered by channel ordered by timestamp ascending
    List<AdminPing> findByChannelOrderByPingTimestampAsc(String channel);

    // Delete selected pings based on list of IDs
    @Transactional
    @Modifying
    @Query("DELETE FROM AdminPing p WHERE p.id IN :ids")
    void deleteSelectedPings(@Param("ids") List<Long> ids);

    // Delete pings older than a specific date (day-wise clean-up)
    @Transactional
    @Modifying
    @Query("DELETE FROM AdminPing p WHERE p.pingTimestamp < :dateTime")
    void deletePingsOlderThan(@Param("dateTime") LocalDateTime dateTime);

    // Clear all messages
    @Transactional
    @Modifying
    @Query("DELETE FROM AdminPing p")
    void clearAllPings();
}
