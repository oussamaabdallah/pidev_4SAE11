import { Component, Input, OnInit, signal, HostListener } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { Router } from '@angular/router';
import { CommonModule, DatePipe } from '@angular/common';
import { AuthService } from '../../../core/services/auth.service';
import { UserService } from '../../../core/services/user.service';
import { NotificationService, AppNotification } from '../../../core/services/notification.service';
import { Button } from '../button/button';

@Component({
  selector: 'app-header',
  imports: [CommonModule, DatePipe, RouterLink, RouterLinkActive, Button],
  templateUrl: './header.html',
  styleUrl: './header.scss',
  standalone: true,
})
export class Header implements OnInit {
  @Input() variant: 'public' | 'dashboard' | 'admin' = 'public';

  mobileMenuOpen = signal(false);
  dashboardMobileOpen = signal(false);
  userMenuOpen = signal(false);
  activeDropdown = signal<string | null>(null);
  isScrolled = signal(false);
  avatarUrl = signal<string | null>(null);

  notifications = signal<AppNotification[]>([]);
  unreadCount = signal(0);
  notificationDropdownOpen = signal(false);
  currentUserId = signal<number | null>(null);

  constructor(
    public auth: AuthService,
    private userService: UserService,
    private notificationService: NotificationService,
    private router: Router
  ) {}

  ngOnInit(): void {
    if (this.auth.isLoggedIn()) {
      const email = this.auth.getPreferredUsername();
      if (email) {
        this.userService.getByEmail(email).subscribe((user) => {
          const url = user?.avatarUrl?.trim() || null;
          setTimeout(() => this.avatarUrl.set(url), 0);
          if (user?.id) {
            this.currentUserId.set(user.id);
            this.loadNotifications();
            this.loadUnreadCount();
          }
        });
      }
    }
  }

  loadNotifications(): void {
    const id = this.currentUserId();
    if (!id) return;
    this.notificationService.getNotifications(id).subscribe((list) => this.notifications.set(list));
  }

  loadUnreadCount(): void {
    const id = this.currentUserId();
    if (!id) return;
    this.notificationService.getUnreadCount(id).subscribe((c) => this.unreadCount.set(c));
  }

  toggleNotificationDropdown(): void {
    this.notificationDropdownOpen.update((v) => !v);
    if (!this.notificationDropdownOpen()) return;
    this.loadNotifications();
    this.loadUnreadCount();
  }

  closeNotificationDropdown(): void {
    this.notificationDropdownOpen.set(false);
  }

  onNotificationClick(n: AppNotification): void {
    const userId = this.currentUserId();
    if (userId) this.notificationService.markAsRead(n.id, userId).subscribe();
    this.notifications.update((list) => list.map((x) => (x.id === n.id ? { ...x, read: true } : x)));
    this.unreadCount.update((c) => Math.max(0, c - 1));
    this.notificationDropdownOpen.set(false);
    if (n.offerId != null) {
      if (this.auth.isFreelancer()) {
        this.router.navigate(['/dashboard/my-offers', n.offerId, 'show']);
      } else {
        this.router.navigate(['/dashboard/browse-offers', n.offerId]);
      }
    }
  }

  get logoRoute(): string {
    if (!this.auth.isLoggedIn()) return '/';
    return this.auth.isAdmin() ? '/admin' : '/dashboard';
  }

  // ── Toggles ────────────────────────────────────────────────

  toggleMobileMenu(): void { this.mobileMenuOpen.update(v => !v); }
  toggleDashboardMobile(): void { this.dashboardMobileOpen.update(v => !v); }
  toggleUserMenu(): void { this.userMenuOpen.update(v => !v); }

  openDropdown(name: string): void { this.activeDropdown.set(name); }
  closeDropdown(): void { this.activeDropdown.set(null); }
  toggleDropdown(name: string): void {
    this.activeDropdown.update(current => current === name ? null : name);
  }

  // ── Search ─────────────────────────────────────────────────

  handleSearch(event: Event, query: string): void {
    event.preventDefault();
    const q = query.trim();
    if (q) {
      this.router.navigate(['/dashboard/search'], { queryParams: { q } });
    }
  }

  // ── Auth ───────────────────────────────────────────────────

  logout(): void {
    this.auth.logout();
    this.userMenuOpen.set(false);
  }

  onAvatarError(): void { this.avatarUrl.set(null); }

  // ── Scroll shadow ──────────────────────────────────────────

  @HostListener('window:scroll')
  onScroll(): void {
    this.isScrolled.set(window.scrollY > 8);
  }

  // ── Outside-click close ────────────────────────────────────

  @HostListener('document:click', ['$event'])
  onDocumentClick(e: MouseEvent): void {
    const target = e.target as HTMLElement;

    if (!target.closest('.user-menu-container')) {
      this.userMenuOpen.set(false);
    }
    if (!target.closest('.notification-dropdown-container')) {
      this.closeNotificationDropdown();
    }
    if (!target.closest('.nav-group')) {
      this.activeDropdown.set(null);
    }
    // Close dashboard mobile drawer when clicking outside the header
    if (!target.closest('.header')) {
      this.dashboardMobileOpen.set(false);
    }
  }
}
