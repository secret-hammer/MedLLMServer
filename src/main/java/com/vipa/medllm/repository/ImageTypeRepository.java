package com.vipa.medllm.repository;

import com.vipa.medllm.model.ImageType;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ImageTypeRepository extends JpaRepository<ImageType, Integer> {

    Optional<ImageType> findByImageTypeId(long imageTypeId);
}
