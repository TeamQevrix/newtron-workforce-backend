package com.newtron.newtron_workforce_backend.common.response;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class MetaInfo {

    private final String apiVersion;
    private final Instant timestamp;
    private final String requestId;
    private final String path;
    private final Long processingTimeMs;
}
