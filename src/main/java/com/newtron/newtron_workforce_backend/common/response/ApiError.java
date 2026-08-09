package com.newtron.newtron_workforce_backend.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiError {

    private final String field;
    private final String code;
    private final String message;
    private final Object rejectedValue;
}
