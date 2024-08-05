package com.vipa.medllm.service.group;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import com.vipa.medllm.dto.request.group.CreateGroupRequest;
import com.vipa.medllm.dto.request.group.UpdateGroupRequest;
import com.vipa.medllm.dto.response.group.ResponseGetGroups;
import com.vipa.medllm.model.ImageGroup;
import com.vipa.medllm.model.Project;
import com.vipa.medllm.repository.ImageGroupRepository;
import org.springframework.stereotype.Service;
import com.vipa.medllm.repository.ProjectRepository;
import com.vipa.medllm.exception.CustomError;
import com.vipa.medllm.exception.CustomException;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
import java.util.*;

@Service
public class GroupService {

    private final ImageGroupRepository imageGroupRepository;
    private final ProjectRepository projectRepository;

    public GroupService(ImageGroupRepository imageGroupRepository, ProjectRepository projectRepository) {
        this.imageGroupRepository = imageGroupRepository;
        this.projectRepository = projectRepository;
    }

    @Transactional
    public ResponseGetGroups searchGroup(Integer  projectId, Integer  groupId, String groupName, String groupDescription) {
        // 第一步：如果groupId不为空，则直接搜索
        if (groupId != null) { //如果groupId不传的话，groupId为空的话
            ImageGroup group = imageGroupRepository.findById(groupId).orElse(null);
            if (group != null && (groupName == null || group.getImageGroupName().contains(groupName))
                    && (groupDescription == null || group.getDescription().contains(groupDescription))
                    && (projectId == null || group.getProject().getProjectId() == projectId)) {
                return new ResponseGetGroups(List.of(new ResponseGetGroups.GroupDTO(
                        group.getImageGroupId(),
                        group.getImageGroupName(),
                        group.getDescription(),
                        group.getProject().getProjectId()
                )));
            } else {
                return new ResponseGetGroups(new ArrayList<>());
            }
        }

        // 第二步：根据projectId和其他条件搜索
        Specification<ImageGroup> spec = (root, query, criteriaBuilder) -> {
            Specification<ImageGroup> criteria = Specification.where(null);

            if (projectId != null) {
                criteria = criteria.and((root1, query1, cb) ->
                        cb.equal(root1.get("project").get("projectId"), projectId));
            }
            if (groupName != null && !groupName.isEmpty()) {
                criteria = criteria.and((root1, query1, cb) ->
                        cb.like(root1.get("imageGroupName"), "%" + groupName + "%"));
            }
            if (groupDescription != null && !groupDescription.isEmpty()) {
                criteria = criteria.and((root1, query1, cb) ->
                        cb.like(root1.get("description"), "%" + groupDescription + "%"));
            }

            return criteria.toPredicate(root, query, criteriaBuilder);
        };

        List<ImageGroup> selectedGroups = imageGroupRepository.findAll(spec, Sort.by("imageGroupId")).stream().distinct().toList();

        // 使用 LinkedHashSet 来存储唯一的 GroupDTO 对象，确保有序性

        // 对 selectedGroups 进行排序，将精确匹配的放前面，模糊匹配的放后面
        List<ResponseGetGroups.GroupDTO> sortedGroups = selectedGroups.stream()
                .map(group -> new ResponseGetGroups.GroupDTO(
                        group.getImageGroupId(),
                        group.getImageGroupName(),
                        group.getDescription(),
                        group.getProject().getProjectId()
                ))
                .sorted((g1, g2) -> {
                    int g1Match = 0, g2Match = 0;

                    if (groupName != null && !groupName.isEmpty()) {
                        if (g1.getImageGroupName().equals(groupName)) {
                            g1Match += 2;
                        } else if (g1.getImageGroupName().contains(groupName)) {
                            g1Match += 1;
                        }
                        if (g2.getImageGroupName().equals(groupName)) {
                            g2Match += 2;
                        } else if (g2.getImageGroupName().contains(groupName)) {
                            g2Match += 1;
                        }
                    }

                    if (groupDescription != null && !groupDescription.isEmpty()) {
                        if (g1.getDescription().equals(groupDescription)) {
                            g1Match += 2;
                        } else if (g1.getDescription().contains(groupDescription)) {
                            g1Match += 1;
                        }
                        if (g2.getDescription().equals(groupDescription)) {
                            g2Match += 2;
                        } else if (g2.getDescription().contains(groupDescription)) {
                            g2Match += 1;
                        }
                    }

                    return Integer.compare(g2Match, g1Match);  // 按匹配程度降序排列
                })
                .toList();

//        Set<ResponseGetGroups.GroupDTO> uniqueGroups = new LinkedHashSet<>(sortedGroups);

        return new ResponseGetGroups(new ArrayList<>(sortedGroups));
    }


    @Transactional
    public void createGroup(CreateGroupRequest createGroupRequest) {
        Project project = projectRepository.findByProjectId(createGroupRequest.getProjectId())
                .orElseThrow(() -> new CustomException(CustomError.PROJECT_NOT_FOUND));

        List<CreateGroupRequest.GroupDetail> targetGroups = createGroupRequest.getTargetGroups();

        for (CreateGroupRequest.GroupDetail targetGroup : targetGroups) {
            ImageGroup imageGroup = new ImageGroup();
            imageGroup.setImageGroupName(targetGroup.getName());
            if (targetGroup.getDescription() != null) imageGroup.setDescription(targetGroup.getDescription());
            imageGroup.setProject(project);

            imageGroupRepository.save(imageGroup);
        }
    }

    @Transactional
    @Retryable(retryFor = { SQLException.class, OptimisticLockingFailureException.class }, maxAttempts = 3, backoff = @Backoff(delay = 100))
    public void updateGroup(UpdateGroupRequest updateGroupRequest) {
        updateGroupRequest.getTargetGroups().forEach(groupDetail -> {
            Optional<ImageGroup> optionalGroup = imageGroupRepository.findAllByImageGroupId(groupDetail.getGroupId());
            if (optionalGroup.isPresent()) {
                ImageGroup group = optionalGroup.get();
                Project currentProject = group.getProject();
                int newProjectId = groupDetail.getProjectId();

                if (currentProject.getProjectId() != newProjectId) {
                    Project newProject = projectRepository.findById(newProjectId)
                            .orElseThrow(() -> new RuntimeException("ProjectId " + groupDetail.getProjectId() + " not found"));
                    group.setProject(newProject);
                }

                group.setImageGroupName(groupDetail.getName());
                group.setDescription(groupDetail.getDescription());
                imageGroupRepository.save(group);
            } else {
                throw new RuntimeException("Group not found with id: " + groupDetail.getGroupId());
            }
        });
    }
}
