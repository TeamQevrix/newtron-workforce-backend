package com.newtron.newtron_workforce_backend.dto.chat;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessageResponse {
    private Long id;
    private Long sessionId;
    private Long senderUserId;
    private String message;
    private String messageType;
    private String attachmentUrl;
    private Instant createdAt;
}
