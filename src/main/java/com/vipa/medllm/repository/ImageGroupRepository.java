package com.vipa.medllm.repository;

import com.vipa.medllm.model.ImageGroup;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImageGroupRepository extends JpaRepository<ImageGroup, Integer> {
    // Additional custom queries can be defined here
}