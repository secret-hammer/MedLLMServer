package com.vipa.medllm.dto.request.task;

import com.fasterxml.jackson.annotation.JsonInclude;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@AllArgsConstructor
public class LiveQAToComputationRequest {
    @NotNull(message = "text cannot be null")
    private String text;

    // image url
    private String image;

    // 切割窗口的左上角横坐标
    private Float x;

    // 切割窗口的左上角纵坐标
    private Float y;

    // 切割窗口的宽度
    private Float width;

    // 切割窗口的高度
    private Float height;
}
