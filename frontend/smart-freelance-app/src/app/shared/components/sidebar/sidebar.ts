import { Component, Input, signal, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../../core/services/auth.service';

interface NavItem {
  label: string;
  route: string;
  icon: string;
  children?: NavItem[];
}

@Component({
  selector: 'app-sidebar',
  imports: [CommonModule, RouterLink, RouterLinkActive],
  templateUrl: './sidebar.html',
  styleUrl: './sidebar.scss',
  standalone: true,
})
export class Sidebar {
  @Input() variant: 'dashboard' | 'admin' = 'dashboard';

  /** Route of the dropdown group currently open (e.g. '/admin/planning'). */
  openDropdownRoute = signal<string | null>(null);

  private router = inject(Router);

  constructor(public auth: AuthService) {}

  isDropdownOpen(item: NavItem): boolean {
    const current = this.router.url;
    const route = item.route;
    const childRoutes = item.children?.map((c) => c.route) ?? [];
    const isActive = current === route || childRoutes.some((r) => current.startsWith(r));
    return this.openDropdownRoute() === route || isActive;
  }

  toggleDropdown(item: NavItem): void {
    const route = item.route;
    this.openDropdownRoute.update((prev) => (prev === route ? null : route));
  }

  get navItems(): NavItem[] {
    if (this.variant === 'admin') {
      return [
        { label: 'Dashboard', route: '/admin', icon: '📊' },
        { label: 'Users', route: '/admin/users', icon: '👥' },
        { label: 'Contracts', route: '/admin/contracts', icon: '📋' },
        { label: 'Offers', route: '/admin/offers', icon: '💼' },
        { label: 'Projects', route: '/admin/projects', icon: '🚀' },
        {
          label: 'Planning',
          route: '/admin/planning',
          icon: '📅',
          children: [
            { label: 'Calendar', route: '/admin/calendar', icon: '📆' },
            { label: 'GitHub', route: '/admin/github', icon: '🐙' },
          ],
        },
        { label: 'Tasks', route: '/admin/tasks', icon: '✅' },
        { label: 'Evaluations', route: '/admin/evaluations', icon: '📝' },
        { label: 'Reviews', route: '/admin/reviews', icon: '⭐' },
        { label: 'Settings', route: '/admin/settings', icon: '⚙️' },
      ];
    }

    // Dashboard (CLIENT/FREELANCER)
    const commonItems: NavItem[] = [
      { label: 'Dashboard', route: '/dashboard', icon: '🏠' },
    ];

    if (this.auth.isClient()) {
      return [
        ...commonItems,
        { label: 'Browse Freelancers', route: '/dashboard/browse-freelancers', icon: '🔍' },
        { label: 'Browse Offers', route: '/dashboard/browse-offers', icon: '💼' },
        { label: 'My Offer Applications', route: '/dashboard/my-offer-applications', icon: '📝' },
        { label: 'Post a Job', route: '/dashboard/post-job', icon: '➕' },
        { label: 'My Projects', route: '/dashboard/my-projects', icon: '📁' },
        { label: 'My Reviews', route: '/dashboard/reviews', icon: '⭐' },
        { label: 'Reviews about me', route: '/dashboard/reviews/about-me', icon: '💬' },
        { label: 'My Contracts', route: '/dashboard/my-contracts', icon: '📋' },
        { label: 'Project Tasks', route: '/dashboard/project-tasks', icon: '✅' },
        { label: 'Track Progress', route: '/dashboard/track-progress', icon: '📊' },
        { label: 'GitHub', route: '/dashboard/github', icon: '🐙' },
        { label: 'Messages', route: '/dashboard/messages', icon: '💬' },
        { label: 'Notifications', route: '/dashboard/notifications', icon: '🔔' },
        { label: 'Profile', route: '/dashboard/profile', icon: '👤' },
        { label: 'Settings', route: '/dashboard/settings', icon: '⚙️' },
      ];
    }

    if (this.auth.isFreelancer()) {
      return [
        ...commonItems,
        { label: 'My Offers', route: '/dashboard/my-offers', icon: '💼' },
        { label: 'Browse Jobs', route: '/dashboard/browse-jobs', icon: '🔍' },
        { label: 'My Applications', route: '/dashboard/my-applications', icon: '📋' },
        { label: 'My Reviews', route: '/dashboard/reviews', icon: '⭐' },
        { label: 'Reviews about me', route: '/dashboard/reviews/about-me', icon: '💬' },
        { label: 'My Contracts', route: '/dashboard/my-contracts', icon: '📋' },
        { label: 'My Tasks', route: '/dashboard/my-tasks', icon: '✅' },
        { label: 'My Progress Updates', route: '/dashboard/progress-updates', icon: '📊' },
        { label: 'GitHub', route: '/dashboard/github', icon: '🐙' },
        { label: 'My Portfolio', route: '/dashboard/my-portfolio', icon: '🎨' },
        { label: 'Messages', route: '/dashboard/messages', icon: '💬' },
        { label: 'Notifications', route: '/dashboard/notifications', icon: '🔔' },
        { label: 'Profile', route: '/dashboard/profile', icon: '👤' },
        { label: 'Settings', route: '/dashboard/settings', icon: '⚙️' },
      ];
    }

    return commonItems;
  }
}
