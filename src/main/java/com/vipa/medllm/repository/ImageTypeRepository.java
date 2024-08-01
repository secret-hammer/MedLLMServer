package com.vipa.medllm.repository;

import com.vipa.medllm.model.ImageType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImageTypeRepository extends JpaRepository<ImageType, Integer> {
    // Additional custom queries can be defined here
}