package tn.esprit.gamification.Services;

import tn.esprit.gamification.Entities.UserLevel;

public interface UserLevelService {
    UserLevel getUserLevel(Long userId);
    void addXp(Long userId, int xp);
}
