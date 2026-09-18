package com.newtron.newtron_workforce_backend.dto.chat;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateChatSessionRequest {
    private Long teamId;
    private Long companyId;
}
