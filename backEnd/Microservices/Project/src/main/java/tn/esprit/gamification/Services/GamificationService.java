package tn.esprit.gamification.Services;

public interface GamificationService {
    void handleProjectCompleted(Long userId);
    void handleProjectCreated(Long userId);
}
