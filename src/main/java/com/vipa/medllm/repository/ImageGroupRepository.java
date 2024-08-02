package com.vipa.medllm.repository;

import com.vipa.medllm.model.ImageGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


//public interface ImageGroupRepository extends JpaRepository<ImageGroup, Integer>, JpaSpecificationExecutor<ImageGroup> {
//    @Query("SELECT ig FROM ImageGroup ig LEFT JOIN FETCH ig.project WHERE ig.project.projectId = ?1")
//    List<ImageGroup> findAllByProjectId(String projectId);
//}
public interface ImageGroupRepository extends JpaRepository<ImageGroup, Integer>, JpaSpecificationExecutor<ImageGroup> {
    boolean existsByImageGroupId(int ImageGroupId);
    Optional<ImageGroup> findAllByImageGroupId(int imageGroupId);
    List<ImageGroup> findAllByProjectProjectId(int projectId); // todo: 可以通过ProjectId找到吗？

}