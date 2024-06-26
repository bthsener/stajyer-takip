package com.batuhansener.stajyerTakip.service;

import com.batuhansener.stajyerTakip.dto.request.UpdateProjectRequest;
import com.batuhansener.stajyerTakip.dto.response.ProjectDto;
import com.batuhansener.stajyerTakip.dto.converter.ProjectDtoConverter;
import com.batuhansener.stajyerTakip.dto.request.CreateProjectRequest;
import com.batuhansener.stajyerTakip.model.Comment;
import com.batuhansener.stajyerTakip.model.Project;
import com.batuhansener.stajyerTakip.model.ProjectStatus;
import com.batuhansener.stajyerTakip.model.User;
import com.batuhansener.stajyerTakip.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserService userService;
    private final ProjectDtoConverter projectDtoConverter;

    public ProjectDto create(CreateProjectRequest request){
        User user = userService.findAuthenticatedUser();

        Project project = Project.builder().name(request.name()).users(new ArrayList<>()).initialDate(LocalDateTime.now())
                .comments(new ArrayList<>()).projectStatus(ProjectStatus.ONGOING).build();
        project = projectRepository.save(project);
        user.getProjects().add(project);
        userService.genericUpdateUser(user);

        return projectDtoConverter.convert(addProjectUser(project, user));
    }

    public Project findProjectById(String id){
        return projectRepository.findById(id).orElseThrow(()->new RuntimeException("proje yok"));
    }

    public void addProjectComment(Project project, Comment comment){
        project.getComments().add(comment);
        projectRepository.saveAndFlush(project);
    }

    public void deleteProjectComment(Comment comment){
        Project project = projectRepository.findProjectByComments(comment);
        project.getComments().remove(comment);
        projectRepository.saveAndFlush(project);
    }

    public Project addProjectUser(Project project, User user){
        project.getUsers().add(user);
        return projectRepository.saveAndFlush(project);
    }

    public Project genericUpdateProject(Project project) {
        return projectRepository.save(project);
    }

    public ProjectDto update(UpdateProjectRequest request, String projectId) {
        Project project = findProjectById(projectId);
        project.setName(request.name());
        if (request.project_status().equals(ProjectStatus.ONGOING.name())){
            project.setProjectStatus(ProjectStatus.ONGOING);
        }else if (request.project_status().equals(ProjectStatus.FINISHED.name())){
            project.setProjectStatus(ProjectStatus.FINISHED);
            project.setFinishDate(LocalDateTime.now());
        }else if (request.project_status().equals(ProjectStatus.DROPPED.name())){
            project.setProjectStatus(ProjectStatus.DROPPED);
        }
        return projectDtoConverter.convert(projectRepository.save(project));
    }

    public List<ProjectDto> getAllProjects() {
        List<Project> projects = projectRepository.findAll();
        return projects.stream().map(projectDtoConverter::convert).collect(Collectors.toList());
    }
}
