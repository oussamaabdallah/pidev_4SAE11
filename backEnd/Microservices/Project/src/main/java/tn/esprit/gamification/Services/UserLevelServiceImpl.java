package tn.esprit.gamification.Services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tn.esprit.gamification.Entities.UserLevel;
import tn.esprit.gamification.Repository.UserLevelRepository;

@Service
public class UserLevelServiceImpl implements UserLevelService {

    @Autowired
    private UserLevelRepository repo;

    @Override
    public UserLevel getUserLevel(Long userId) {
        return repo.findByUserId(userId)
                .orElseGet(() -> {
                    UserLevel ul = new UserLevel();
                    ul.setUserId(userId);
                    return repo.save(ul);
                });
    }

    @Override
    public void addXp(Long userId, int xp) {
        UserLevel ul = getUserLevel(userId);

        ul.setXp(ul.getXp() + xp);

        int level = ul.getXp() / 100 + 1;
        ul.setLevel(level);

        repo.save(ul);
    }
}
