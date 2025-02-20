package com.kliksigurnost.demo.repository;

import com.kliksigurnost.demo.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NotificationRepository  extends JpaRepository<Notification, Integer> {
    List<Notification> findByUserId(Integer user_id);

    List<Notification> findByUserIdAndIsSeen(Integer user_id, Boolean isSeen);

    @Modifying
    @Query("UPDATE Notification n SET n.isSeen = true WHERE n.notificationId IN :notificationIds")
    void markNotificationsAsSeen(@Param("notificationIds") List<String> notificationIds);
}