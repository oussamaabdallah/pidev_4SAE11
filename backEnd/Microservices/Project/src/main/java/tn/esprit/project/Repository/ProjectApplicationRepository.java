package tn.esprit.project.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tn.esprit.project.Entities.ProjectApplication;

import java.util.List;

@Repository
public interface ProjectApplicationRepository extends JpaRepository<ProjectApplication, Long> {
    List<ProjectApplication> findByProjectId(Long projectId);
    List<ProjectApplication> findByFreelanceId(Long freelanceId);

    /** Fetch applications with project loaded in one query to avoid LazyInitializationException. */
    @Query("SELECT a FROM ProjectApplication a JOIN FETCH a.project WHERE a.freelanceId = :freelanceId")
    List<ProjectApplication> findByFreelanceIdWithProject(@Param("freelanceId") Long freelanceId);
}
