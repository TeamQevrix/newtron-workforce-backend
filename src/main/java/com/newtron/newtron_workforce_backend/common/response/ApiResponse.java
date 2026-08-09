package com.newtron.newtron_workforce_backend.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"success", "message", "data", "errors", "meta"})
public class ApiResponse<T> {

    private final boolean success;
    private final String message;
    private final T data;
    private final List<ApiError> errors;
    private final MetaInfo meta;
}
