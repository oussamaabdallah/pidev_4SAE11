package tn.esprit.gamification.Controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tn.esprit.gamification.Services.GamificationService;

@RestController
@RequestMapping("/api/gamification")
public class GamificationController {

    @Autowired
    private GamificationService service;

    @PostMapping("/project-completed")
    public void projectCompleted(@RequestParam Long userId) {
        service.handleProjectCompleted(userId);
    }

    @PostMapping("/project-created")
    public void projectCreated(@RequestParam Long userId) {
        service.handleProjectCreated(userId);
    }
}
