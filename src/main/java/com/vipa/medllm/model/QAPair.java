package com.vipa.medllm.model;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.sql.Timestamp;

@Data
@Document(collection = "qa_pairs")
public class QAPair {
    @Id
    private ObjectId qaPairId;

    @DBRef
    private Session session;

    @NotNull(message = "LLM task type id is required")
    private Integer llmTaskTypeId;

    private String question;

    private String answer;

    private Timestamp questionTime;

    private Timestamp answerTime;

    @Version
    private Integer version;

    public String getQAPairId() {
        return qaPairId.toHexString();
    }
}