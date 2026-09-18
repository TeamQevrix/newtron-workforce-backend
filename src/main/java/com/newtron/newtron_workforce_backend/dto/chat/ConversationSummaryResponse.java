package com.newtron.newtron_workforce_backend.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConversationSummaryResponse {
    private Long sessionId;
    private Long companyId;
    private String companyName;
    private String latestMessage;
    private Instant latestMessageTimestamp;
}
