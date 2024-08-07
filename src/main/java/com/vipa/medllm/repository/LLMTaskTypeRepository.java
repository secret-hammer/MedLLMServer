package com.vipa.medllm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.vipa.medllm.model.LLMTaskType;

public interface LLMTaskTypeRepository
        extends JpaRepository<LLMTaskType, Integer>, JpaSpecificationExecutor<LLMTaskType> {
    Integer countByIsPreProcessTask(Boolean isPreProcessTask);
}
