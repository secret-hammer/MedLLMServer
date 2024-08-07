package com.vipa.medllm.model;

import lombok.Data;

import java.sql.Timestamp;
import java.util.*;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.validation.constraints.NotNull;

@Data
@Document(collection = "sessions")
public class Session {
    @Id
    private ObjectId sessionId;

    @NotNull(message = "Image Id is required")
    private Integer imageId;

    private Timestamp createdTime;

    private Timestamp updatedTime;

    @JsonIgnore
    @DBRef // 这是一个引用指向大模型预推理问答对的列表
    private Map<Integer, QAPair> qaPairPreInferenceTaskMap = new HashMap<>();

    @DBRef // 这是一个引用指向历史问答对的列表，按顺序存放
    private List<QAPair> qaPairHistoryList = new ArrayList<>();

    @NotNull(message = "User Id is required")
    private Integer userId; // 直接关联用户ID，避免多级关联访问

    @NotNull(message = "Status is required")
    // 0:代表刚刚上传；
    // 1:代表大模型预推理结束（病理图处理未结束）
    // 2:代表病理图处理结束（大模型预推理未结束）
    // 3:代表病理图预处理全部完成，处于可用交互状态
    // 4:病理图已经被删除，会话处于废弃状态，但仍然保留
    private Integer status;

    @Version
    private Integer version;

    public String getSessionId() {
        return sessionId.toHexString();
    }
}
