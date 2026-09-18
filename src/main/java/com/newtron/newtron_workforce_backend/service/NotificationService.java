package com.newtron.newtron_workforce_backend.service;

import com.newtron.newtron_workforce_backend.auth.entity.User;
import com.newtron.newtron_workforce_backend.dto.NotificationDto;
import java.util.List;

public interface NotificationService {
    List<NotificationDto> getCurrentUserNotifications(User currentUser);
    long getCurrentUserUnreadCount(User currentUser);
    NotificationDto markNotificationAsRead(Long notificationId, User currentUser);
    void markAllCurrentUserNotificationsAsRead(User currentUser);
    void deleteNotification(Long notificationId, User currentUser);
}
