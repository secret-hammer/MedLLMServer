package com.vipa.medllm.dto.middto.session;

import com.vipa.medllm.model.Session;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import java.sql.Timestamp;

@Data
@AllArgsConstructor
public class CreateQAPairDto {

    @NotNull(message = "Session cannot be null")
    private Session session;

    @NotNull(message = "LLM task type Id cannot be null")
    private Integer llmTaskTypeId;

    private String question;
    private String answer;
    private Timestamp questionTime;
    private Timestamp answerTime;

    public CreateQAPairDto(Session session, Integer llmTaskTypeId, String question, Timestamp questionTime) {
        this.session = session;
        this.llmTaskTypeId = llmTaskTypeId;
        this.question = question;
        this.questionTime = questionTime;
    }
}
