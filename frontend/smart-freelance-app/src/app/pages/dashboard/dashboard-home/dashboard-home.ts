import { Component, OnInit, signal, computed, inject, ChangeDetectorRef } from '@angular/core';
import { AuthService } from '../../../core/services/auth.service';
import { ProjectService, Project } from '../../../core/services/project.service';
import { UserService } from '../../../core/services/user.service';
import { RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';
import { ProjectsFeed } from '../../../shared/components/projects-feed/projects-feed.component';
import { ProjectFeed } from '../../../shared/models/project-feed';
import { Card } from '../../../shared/components/card/card';

@Component({
  selector: 'app-dashboard-home',
  standalone: true,
  imports: [ProjectsFeed, Card, RouterModule, CommonModule],
  templateUrl: './dashboard-home.html',
  styleUrl: './dashboard-home.scss',
})
export class DashboardHome implements OnInit {
  // Injected services
  private projectService = inject(ProjectService);
  public auth = inject(AuthService);
  private us = inject(UserService);
  private cdr = inject(ChangeDetectorRef);

  // User role and recommendations (from incoming)
  userRole: string | null = null;
  recommendedProjects: Project[] = [];
  isLoadingRecommendations = false;

  // Infinite scroll projects (from current)
  private allProjects = signal<ProjectFeed[]>([]);
  private page = signal(1);
  readonly pageSize = 6;
  isLoading = signal(false);
  isFetching = signal(true);

  // Derived signals
  displayedProjects = computed(() =>
    this.allProjects().slice(0, this.page() * this.pageSize)
  );

  hasMore = computed(() =>
    this.page() * this.pageSize < this.allProjects().length
  );

  ngOnInit(): void {
    // Handle both freelancer recommendations AND general projects feed
    this.userRole = this.auth.getUserRole();

    if (this.userRole === 'FREELANCER') {
      const email = this.auth.getPreferredUsername();
      if (email) {
        this.us.getByEmail(email).subscribe(user => {
          if (user?.id) {
            this.loadRecommendations(user.id);
          }
        });
      }
    }

    // Always load general projects feed (works for all roles)
    this.loadAllProjects();
  }

  // Recommendations logic (from incoming)
  loadRecommendations(userId: number): void {
    this.isLoadingRecommendations = true;
    this.cdr.detectChanges();

    this.projectService.getRecommendedProjects(userId).subscribe({
      next: (projects) => {
        this.recommendedProjects = projects || [];
        this.isLoadingRecommendations = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.isLoadingRecommendations = false;
        this.cdr.detectChanges();
      }
    });
  }

  // Projects feed logic (from current)
  private loadAllProjects(): void {
    this.projectService.getAllProjects().subscribe({
      next: (projects) => {
        const open = projects.filter(p => !p.status || p.status === 'OPEN');
        this.allProjects.set(open.map((p, i) => this.toFeed(p, i)));
        this.isFetching.set(false);
      },
      error: () => {
        this.isFetching.set(false);
      },
    });
  }

  loadMoreProjects(): void {
    if (!this.hasMore() || this.isLoading()) return;

    this.isLoading.set(true);
    setTimeout(() => {
      this.page.update(p => p + 1);
      this.isLoading.set(false);
    }, 400);
  }

  // Data transform utilities
  private toFeed(p: Project, index: number): ProjectFeed {
    return {
      id: String(p.id ?? index),
      title: p.title,
      budget: p.budget ?? 0,
      client: {
        name: 'Project Owner',
        avatar: `https://api.dicebear.com/7.x/avataaars/svg?seed=${p.clientId ?? index}`,
        rating: parseFloat((4.2 + Math.random() * 0.8).toFixed(1)),
      },
      description: p.description,
      skills: this.parseSkills(p.skillsRequiered),
      postedAgo: this.timeAgo(p.createdAt),
      category: p.category,
    };
  }

  private parseSkills(s: string | string[] | null | undefined): string[] {
    if (!s) return [];
    if (Array.isArray(s)) return s;
    return s.split(',').map(x => x.trim()).filter(Boolean);
  }

  private timeAgo(dateStr?: string): string {
    if (!dateStr) return 'recently';
    const diff = Date.now() - new Date(dateStr).getTime();
    const h = Math.floor(diff / 3_600_000);
    const d = Math.floor(h / 24);
    if (d > 0) return `${d}d ago`;
    if (h > 0) return `${h}h ago`;
    return 'just now';
  }
}
