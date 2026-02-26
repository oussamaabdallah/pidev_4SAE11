package tn.esprit.project.Services;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.project.Entities.Project;
import tn.esprit.project.Entities.ProjectApplication;
import tn.esprit.project.Repository.ProjectApplicationRepository;
import tn.esprit.project.Repository.ProjectRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class IProjectServicelmp implements IProjectService{
    private ProjectRepository projectRepository;
    private ProjectApplicationRepository projectApplicationRepository;
    @Override
    public Project addProject(Project project) {
        return projectRepository.save(project);
    }
    @Override
    public Project updateProject(Project project) {
        return projectRepository.save(project);
    }

    @Override
    public void deleteProject(Long id) {
        projectRepository.deleteById(id);
    }
    @Override
    public Project getProjectById(Long id) {
        return projectRepository.findById(id).orElse(null);
    }

    @Override
    public List<Project> getAllProjects() {
        return projectRepository.findAll();
    }

    @Override
    public List<Project> getProjectsByClientId(Long clientId) {
        return projectRepository.findByClientId(clientId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Project> getProjectsByFreelancerId(Long freelancerId) {
        return projectApplicationRepository.findByFreelanceIdWithProject(freelancerId).stream()
                .map(ProjectApplication::getProject)
                .distinct()
                .collect(Collectors.toList());
    }

}
