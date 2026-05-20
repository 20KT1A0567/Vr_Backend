package com.vrtechnologies.vrtech.repository;

import com.vrtechnologies.vrtech.entity.NotificationLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {
    List<NotificationLog> findTop100ByOrderByCreatedAtDescIdDesc();
    List<NotificationLog> findTop50ByStatusInAndNextAttemptAtLessThanEqualOrderByCreatedAtAscIdAsc(List<String> statuses, LocalDateTime nextAttemptAt);
    long countByIsReadFalse();
}
