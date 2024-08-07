package com.vipa.medllm.service.project;

import com.vipa.medllm.util.DirectoryUtil;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vipa.medllm.dto.request.group.CreateGroupRequest;
import com.vipa.medllm.dto.request.group.CreateGroupRequest.GroupDetail;
import com.vipa.medllm.dto.request.project.CreateProjectInfo;
import com.vipa.medllm.dto.request.project.UpdateProjectInfo;
import com.vipa.medllm.exception.CustomError;
import com.vipa.medllm.exception.CustomException;
import com.vipa.medllm.model.ImageGroup;
import com.vipa.medllm.model.ImageType;
import com.vipa.medllm.model.Project;
import com.vipa.medllm.model.User;
import com.vipa.medllm.repository.ImageGroupRepository;
import com.vipa.medllm.repository.ImageTypeRepository;
import com.vipa.medllm.repository.ProjectRepository;
import com.vipa.medllm.repository.UserRepository;
import com.vipa.medllm.service.group.GroupService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Paths;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProjectService {

    private final GroupService groupService;

    private final ProjectRepository projectRepository;
    private final ImageGroupRepository imageGroupRepository;
    private final UserRepository userRepository;
    private final ImageTypeRepository imageTypeRepository;

    @Value("${medllm.projects.resource.path}")
    private String projectResourcePath;

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
                    .orElseThrow(() -> new CustomException(CustomError.IMAGETYPE_NOT_FOUND));

            project.setImageType(imageType);
            projectRepository.save(project);

            // 在静态资源服务器上创建数据集文件夹
            String folderPath = String.format(projectResourcePath + "/projects/%d", project.getProjectId());

            Path dir = Paths.get(folderPath);
            if (Files.exists(dir)) {
                log.error(
                        "com.vipa.medllm.service.project.createProjects: Project folder already exists: " + folderPath);
            } else {
                try {
                    DirectoryUtil.createDirectory(dir);
                } catch (IOException e) {
                    log.error(
                            "com.vipa.medllm.service.project.createProjects: Error creating project folder: "
                                    + folderPath);
                }
            }

            // 同时创建一个默认的分组
            CreateGroupRequest createGroupRequest = new CreateGroupRequest();
            createGroupRequest.setProjectId(project.getProjectId());
            GroupDetail defaultGroup = new GroupDetail();
            defaultGroup.setName("默认组");
            defaultGroup.setDescription("默认组");
            createGroupRequest.setTargetGroups(List.of(defaultGroup));
            groupService.createGroup(createGroupRequest);
        }
    }

    @Transactional
    @Retryable(retryFor = {
            OptimisticLockingFailureException.class }, maxAttempts = 3, backoff = @Backoff(delay = 100))
    public void updateProjects(List<UpdateProjectInfo> projectInfoList) {
        // 批量更新数据集,按事务处理
        for (UpdateProjectInfo updateProjectInfo : projectInfoList) {
            Project project = projectRepository.findByProjectId(updateProjectInfo.getProjectId())
                    .orElseThrow(() -> new CustomException(CustomError.PROJECT_NOT_FOUND));

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

    @Transactional
    public void deleteProject(Integer projectId) {
        Optional<Project> optionalProject = projectRepository.findById(projectId);

        if (optionalProject.isPresent()) {
            List<ImageGroup> imageGroups = imageGroupRepository.findAllByProjectProjectId(projectId);
            // 删除所有的组
            for (ImageGroup imageGroup : imageGroups) {
                groupService.deleteGroup(imageGroup.getImageGroupId());
            }
            projectRepository.delete(optionalProject.get());
        } else {
            throw new CustomException(CustomError.PROJECT_NOT_FOUND);
        }
    }

}
