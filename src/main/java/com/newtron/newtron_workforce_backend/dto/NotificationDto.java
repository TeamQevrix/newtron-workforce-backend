package com.newtron.newtron_workforce_backend.dto;

import lombok.*;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationDto {
    private Long id;
    private String title;
    private String description;
    private Boolean isRead;
    private String category;
    private String priority;
    private String deepLink;
    private Instant createdAt;
}
