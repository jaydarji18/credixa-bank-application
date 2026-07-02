package com.credixa.repository;

import com.credixa.entity.Notification;
import com.credixa.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUserOrderByCreatedAtDesc(User user);
    Page<Notification> findByUser(User user, Pageable pageable);
    Page<Notification> findByUserAndIsRead(User user, boolean isRead, Pageable pageable);
    Page<Notification> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    long countByUserAndIsReadFalse(User user);
    long countByUserIdAndIsReadFalse(Long userId);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.user = :user AND n.isRead = false")
    int markAllAsRead(@Param("user") User user);
}
