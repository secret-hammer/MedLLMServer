package com.vipa.medllm.dto.response.task;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LiveQAResponse {

    // 整张推理为0，区域推理为1
    private Integer type;

    // 模型的回答
    private String gpt;
}
