package com.kliksigurnost.demo.service;

import com.kliksigurnost.demo.model.CloudflareDevice;
import com.kliksigurnost.demo.model.Notification;
import com.kliksigurnost.demo.model.User;

import java.util.List;

public interface CloudflareNotificationService {
    public void checkBlockedContent();

    List<Notification> getNotificationsByUser();

    List<Notification> getUnseenNotificationsByUser();

    Integer getUnseenNotificationCountByUser();

    void markNotificationsAsSeen(List<Integer> notificationIds);

    void deleteNotification(Integer id);

    void createNotificationForDevices(List<CloudflareDevice> devicesList, User user);
}
