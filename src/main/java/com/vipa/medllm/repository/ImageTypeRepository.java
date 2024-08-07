package com.vipa.medllm.repository;

import com.vipa.medllm.model.ImageType;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ImageTypeRepository extends JpaRepository<ImageType, Integer> {

    Optional<ImageType> findByImageTypeId(long imageTypeId);
}
