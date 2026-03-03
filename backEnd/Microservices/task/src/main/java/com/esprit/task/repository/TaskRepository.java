package com.esprit.task.repository;

import com.esprit.task.entity.Task;
import com.esprit.task.entity.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long>, JpaSpecificationExecutor<Task> {

    List<Task> findByProjectId(Long projectId);

    List<Task> findByProjectIdOrderByOrderIndexAsc(Long projectId);

    List<Task> findByContractId(Long contractId);

    List<Task> findByAssigneeId(Long assigneeId);

    List<Task> findByAssigneeIdOrderByProjectIdAscOrderIndexAsc(Long assigneeId);

    List<Task> findByContractIdOrderByProjectIdAscOrderIndexAsc(Long contractId);

    List<Task> findByParentTaskId(Long parentTaskId);

    List<Task> findByProjectIdAndParentTaskIdIsNullOrderByOrderIndexAsc(Long projectId);

    @Query("SELECT t FROM Task t WHERE t.dueDate IS NOT NULL AND t.dueDate < :today AND t.status NOT IN ('DONE', 'CANCELLED') ORDER BY t.projectId, t.orderIndex, t.createdAt DESC")
    List<Task> findOverdueTasks(@Param("today") LocalDate today);

    @Query("SELECT t FROM Task t WHERE t.projectId = :projectId AND t.dueDate IS NOT NULL AND t.dueDate < :today AND t.status NOT IN ('DONE', 'CANCELLED') ORDER BY t.orderIndex, t.createdAt DESC")
    List<Task> findOverdueTasksByProject(@Param("projectId") Long projectId, @Param("today") LocalDate today);

    @Query("SELECT t FROM Task t WHERE t.assigneeId = :assigneeId AND t.dueDate IS NOT NULL AND t.dueDate < :today AND t.status NOT IN ('DONE', 'CANCELLED') ORDER BY t.projectId, t.orderIndex, t.createdAt DESC")
    List<Task> findOverdueTasksByAssignee(@Param("assigneeId") Long assigneeId, @Param("today") LocalDate today);

    @Query("SELECT t FROM Task t WHERE t.dueDate IS NOT NULL AND t.dueDate >= :start AND t.dueDate <= :end ORDER BY t.projectId, t.orderIndex, t.createdAt DESC")
    List<Task> findByDueDateBetween(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query("SELECT t FROM Task t WHERE t.dueDate IS NOT NULL AND t.dueDate >= :start AND t.dueDate <= :end AND t.assigneeId = :userId ORDER BY t.projectId, t.orderIndex, t.createdAt DESC")
    List<Task> findByDueDateBetweenAndAssigneeId(@Param("start") LocalDate start, @Param("end") LocalDate end, @Param("userId") Long userId);

    @Query("SELECT COALESCE(MAX(t.orderIndex), 0) FROM Task t WHERE t.projectId = :projectId AND t.parentTaskId IS NULL")
    Integer findMaxOrderIndexByProject(@Param("projectId") Long projectId);

    long countByProjectId(Long projectId);

    long countByProjectIdAndStatus(Long projectId, TaskStatus status);
}
