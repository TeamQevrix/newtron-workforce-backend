package com.newtron.newtron_workforce_backend.service;

import com.newtron.newtron_workforce_backend.auth.entity.User;
import com.newtron.newtron_workforce_backend.entity.Notification;
import com.newtron.newtron_workforce_backend.enums.NotificationCategory;
import com.newtron.newtron_workforce_backend.enums.NotificationPriority;
import com.newtron.newtron_workforce_backend.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationHelper {

    private final NotificationRepository notificationRepository;

    public void sendNotification(User user, String title, String description,
                                 NotificationCategory category, NotificationPriority priority,
                                 String deepLink) {
        Notification notification = Notification.builder()
                .user(user)
                .title(title)
                .description(description)
                .isRead(false)
                .category(category)
                .priority(priority)
                .deepLink(deepLink)
                .build();
        notification.setDeleted(false);
        notificationRepository.save(notification);
    }
}
