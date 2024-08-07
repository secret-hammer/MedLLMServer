package com.vipa.medllm.repository;

import com.vipa.medllm.model.Image;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ImageRepository extends JpaRepository<Image, Integer> {
    // Additional custom queries can be defined here
    List<Image> findByImageGroupImageGroupId(Integer imageGroupId);

    List<Integer> findImageIdByImageGroupImageGroupId(Integer groupId);
}