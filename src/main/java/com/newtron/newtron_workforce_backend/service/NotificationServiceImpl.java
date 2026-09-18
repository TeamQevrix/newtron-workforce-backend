package com.newtron.newtron_workforce_backend.service;

import com.newtron.newtron_workforce_backend.auth.entity.User;
import com.newtron.newtron_workforce_backend.common.exception.ResourceNotFoundException;
import com.newtron.newtron_workforce_backend.dto.NotificationDto;
import com.newtron.newtron_workforce_backend.entity.Notification;
import com.newtron.newtron_workforce_backend.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;

    @Override
    @Transactional(readOnly = true)
    public List<NotificationDto> getCurrentUserNotifications(User currentUser) {
        List<Notification> notifications = notificationRepository.findByUserIdAndDeletedFalseOrderByCreatedAtDesc(currentUser.getId());
        return notifications.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public long getCurrentUserUnreadCount(User currentUser) {
        return notificationRepository.countByUserIdAndIsReadFalseAndDeletedFalse(currentUser.getId());
    }

    @Override
    @Transactional
    public NotificationDto markNotificationAsRead(Long notificationId, User currentUser) {
        Notification notification = notificationRepository.findByIdAndUserIdAndDeletedFalse(notificationId, currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("NOTIFICATION_NOT_FOUND", "Notification not found or access denied"));

        notification.setIsRead(true);
        Notification saved = notificationRepository.save(notification);
        return mapToDto(saved);
    }

    @Override
    @Transactional
    public void markAllCurrentUserNotificationsAsRead(User currentUser) {
        List<Notification> notifications = notificationRepository.findByUserIdAndDeletedFalseOrderByCreatedAtDesc(currentUser.getId());
        boolean hasChanges = false;
        for (Notification n : notifications) {
            if (!n.getIsRead()) {
                n.setIsRead(true);
                hasChanges = true;
            }
        }
        if (hasChanges) {
            notificationRepository.saveAll(notifications);
        }
    }

    @Override
    @Transactional
    public void deleteNotification(Long notificationId, User currentUser) {
        Notification notification = notificationRepository.findByIdAndUserIdAndDeletedFalse(notificationId, currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("NOTIFICATION_NOT_FOUND", "Notification not found or access denied"));

        notification.delete();
        notificationRepository.save(notification);
    }

    private NotificationDto mapToDto(Notification entity) {
        if (entity == null) return null;
        return NotificationDto.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .isRead(entity.getIsRead())
                .category(entity.getCategory().name())
                .priority(entity.getPriority().name())
                .deepLink(entity.getDeepLink())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
