package com.vipa.medllm.dto.middto.task;

import java.sql.Timestamp;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TaskProcessDto {

    // 0:未开始 1:进行中 2:已完成 3:失败
    private Integer status;

    // 0 - 1
    private Float progress;

    // 任务执行结果
    private String message;

    private Timestamp startTime;

    private Timestamp updateTime;

    public TaskProcessDto() {
        this.status = 0;
        this.progress = 0.0f;
        this.message = "";
        Timestamp currentTimestamp = new Timestamp(System.currentTimeMillis());
        this.startTime = currentTimestamp;
        this.updateTime = currentTimestamp;
    }
}
