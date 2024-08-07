package com.vipa.medllm.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "LLMTaskType")
public class LLMTaskType {
    @Id
    private Integer llmTaskTypeId;

    @Column(nullable = false, length = 50)
    private String llmTaskTypeName;

    @Column(nullable = false)
    private Boolean isPreProcessTask;

    @Column(nullable = false, length = 2000)
    private String prompt;

    @Column(nullable = false, length = 2000)
    private String description;
}
