package com.vipa.medllm.dto.request.group;

import com.fasterxml.jackson.annotation.JsonInclude;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DeleteProjectRequest {
    @NotNull(message = "Group ID cannot be null")
    private Integer projectId;
}
