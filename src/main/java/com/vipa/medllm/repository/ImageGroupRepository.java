package com.vipa.medllm.repository;

import com.vipa.medllm.model.ImageGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface ImageGroupRepository extends JpaRepository<ImageGroup, Integer>, JpaSpecificationExecutor<ImageGroup> {
    Optional<ImageGroup> findAllByImageGroupId(Integer imageGroupId);

    List<ImageGroup> findAllByProjectProjectId(Integer projectId);

}