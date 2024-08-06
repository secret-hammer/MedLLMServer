package com.vipa.medllm.service.project;

import com.vipa.medllm.dto.request.group.DeleteGroupRequest;
import com.vipa.medllm.model.*;
import com.vipa.medllm.repository.*;
import com.vipa.medllm.service.image.ImageService;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vipa.medllm.dto.request.project.CreateProjectInfo;
import com.vipa.medllm.dto.request.project.UpdateProjectInfo;

import lombok.AllArgsConstructor;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class ProjectService {
    private ProjectRepository projectRepository;
    private UserRepository userRepository;
    private ImageTypeRepository imageTypeRepository;
    private final ImageGroupRepository imageGroupRepository;
    private final ImageRepository imageRepository;
    private final ImageService imageService;

    @Transactional
    public void createProjects(List<CreateProjectInfo> projectInfoList) {
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String usernameOrEmail = userDetails.getUsername();
        User user = userRepository.findByUsernameOrEmail(usernameOrEmail, usernameOrEmail).get();

        // 批量创建数据集,按事务处理
        for (CreateProjectInfo createProjectInfo : projectInfoList) {
            Project project = new Project();
            project.setUser(user);
            project.setProjectName(createProjectInfo.getProjectName());
            project.setDescription(createProjectInfo.getProjectDescription());

            ImageType imageType = imageTypeRepository.findByImageTypeId(createProjectInfo.getImageTypeId())
                    .orElseThrow(() -> new RuntimeException(
                            String.format("ImageType with id %d not found", createProjectInfo.getImageTypeId())));

            project.setImageType(imageType);
            projectRepository.save(project);
        }
    }

    @Transactional
    @Retryable(retryFor = {SQLException.class,
            OptimisticLockingFailureException.class}, maxAttempts = 3, backoff = @Backoff(delay = 100))
    public void updateProjects(List<UpdateProjectInfo> projectInfoList) {
        // 批量更新数据集,按事务处理
        for (UpdateProjectInfo updateProjectInfo : projectInfoList) {
            Project project = projectRepository.findByProjectId(updateProjectInfo.getProjectId())
                    .orElseThrow(() -> new RuntimeException(
                            String.format("Project with id %d not found", updateProjectInfo.getProjectId())));

            project.setProjectName(updateProjectInfo.getNewProjectName());
            project.setDescription(updateProjectInfo.getNewProjectDescription());

            projectRepository.save(project);
        }
    }

    @Transactional
    public List<Project> searchProjects(Integer projectId, String projectName) {
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        String usernameOrEmail = userDetails.getUsername();
        Optional<User> optionalUser = userRepository.findByUsernameOrEmail(usernameOrEmail, usernameOrEmail);

        Specification<Project> spec = Specification.where(null);

        spec = spec.and((root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("user"), optionalUser.get()));
        if (projectId != null) {
            spec = spec.and((root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("projectId"), projectId));
        }
        if (projectName != null && !projectName.isEmpty()) {
            spec = spec.and((root, query, criteriaBuilder) -> criteriaBuilder.like(root.get("projectName"),
                    "%" + projectName + "%"));
        }

        return projectRepository.findAll(spec);
    }

    // deleteProject: 删除单个project
    @Transactional
    public void deleteProject(Integer projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        List<ImageGroup> imageGroups = imageGroupRepository.findAllByProjectProjectId(projectId);
        for (ImageGroup imageGroup : imageGroups) {
            DeleteGroupRequest deleteGroupRequest = new DeleteGroupRequest();
            deleteGroupRequest.setGroupId(imageGroup.getImageGroupId());

            deleteGroup(deleteGroupRequest);
        }

        projectRepository.delete(project);
    }

    public void deleteGroup(DeleteGroupRequest deleteGroupRequest) {
        ImageGroup imageGroup = imageGroupRepository.findById(deleteGroupRequest.getGroupId())
                .orElseThrow(() -> new RuntimeException("Image group not found"));

        List<Image> images = imageRepository.findByImageGroupImageGroupId(deleteGroupRequest.getGroupId());
        for (Image image : images) {
            ImageService.deleteImageFolder(image);
            imageRepository.delete(image);
        }

        imageGroupRepository.delete(imageGroup);
    }

}
