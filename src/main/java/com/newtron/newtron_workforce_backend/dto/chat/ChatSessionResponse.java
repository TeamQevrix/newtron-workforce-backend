package com.newtron.newtron_workforce_backend.dto.chat;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatSessionResponse {
    private Long id;
    private Long jobId;
    private Long teamId;
    private Long applicationId;
}
