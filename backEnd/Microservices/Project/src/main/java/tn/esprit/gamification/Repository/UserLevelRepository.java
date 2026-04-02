package tn.esprit.gamification.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.gamification.Entities.UserLevel;

import java.util.Optional;

public interface UserLevelRepository extends JpaRepository<UserLevel, Long> {
    Optional<UserLevel> findByUserId(Long userId);
}