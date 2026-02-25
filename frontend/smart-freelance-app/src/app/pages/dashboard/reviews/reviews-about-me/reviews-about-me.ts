import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { StarRating } from '../../../../shared/components/star-rating/star-rating';
import { AuthService } from '../../../../core/services/auth.service';
import { UserService, User } from '../../../../core/services/user.service';
import { ReviewService, Review, ReviewStats } from '../../../../core/services/review.service';
import { ProjectService } from '../../../../core/services/project.service';
import { Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';

@Component({
  selector: 'app-reviews-about-me',
  standalone: true,
  imports: [CommonModule, StarRating],
  templateUrl: './reviews-about-me.html',
  styleUrl: './reviews-about-me.scss',
})
export class ReviewsAboutMe implements OnInit, OnDestroy {
  reviews: Review[] = [];
  loading = true;
  errorMessage = '';
  currentUser: User | null = null;
  reviewerNames: Record<number, string> = {};
  projectTitles: Record<number, string> = {};
  stats: ReviewStats | null = null;
  searchTerm = '';
  ratingFilter: number | null = null;
  page = 0;
  size = 10;
  totalElements = 0;
  totalPages = 0;
  pageSizes = [5, 10, 20, 50];
  private searchSubject = new Subject<string>();

  constructor(
    private auth: AuthService,
    private userService: UserService,
    private reviewService: ReviewService,
    private projectService: ProjectService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    const email = this.auth.getPreferredUsername();
    if (!email) {
      this.loading = false;
      this.errorMessage = 'You must be logged in.';
      this.cdr.detectChanges();
      return;
    }
    this.searchSubject.pipe(debounceTime(300), distinctUntilChanged()).subscribe((term) => {
      this.searchTerm = term;
      this.page = 0;
      this.load();
    });
    this.userService.getByEmail(email).subscribe((user) => {
      this.currentUser = user ?? null;
      if (this.currentUser) {
        this.load();
        this.loadStats();
      } else {
        this.loading = false;
        this.errorMessage = 'Could not load your profile.';
        this.cdr.detectChanges();
      }
    });
  }

  ngOnDestroy(): void {
    this.searchSubject.complete();
  }

  onSearchInput(value: string): void {
    this.searchSubject.next(value);
  }

  onRatingFilterChange(): void {
    this.page = 0;
    this.load();
  }

  load(): void {
    if (!this.currentUser?.id) return;
    this.loading = true;
    this.errorMessage = '';
    this.reviewService
      .getByRevieweeIdPage(this.currentUser.id, {
        page: this.page,
        size: this.size,
        search: this.searchTerm || undefined,
        rating: this.ratingFilter ?? undefined,
      })
      .subscribe({
        next: (res) => {
          if (res) {
            this.reviews = res.content ?? [];
            this.totalElements = res.totalElements;
            this.totalPages = res.totalPages;
            this.loadReviewerNames();
            this.loadProjectTitles();
          } else {
            this.reviews = [];
            this.totalElements = 0;
            this.totalPages = 0;
          }
          this.loading = false;
          this.cdr.detectChanges();
        },
        error: () => {
          this.errorMessage = 'Failed to load reviews.';
          this.loading = false;
          this.cdr.detectChanges();
        },
      });
  }

  loadStats(): void {
    if (!this.currentUser?.id) return;
    this.reviewService.getStatsByReviewee(this.currentUser.id).subscribe((s) => {
      this.stats = s ?? null;
      this.cdr.detectChanges();
    });
  }

  private loadReviewerNames(): void {
    const ids = [...new Set(this.reviews.map((r) => r.reviewerId))];
    ids.forEach((id) => {
      if (this.reviewerNames[id]) return;
      this.userService.getById(id).subscribe((u) => {
        if (u) this.reviewerNames[id] = (u.firstName + ' ' + u.lastName).trim() || u.email;
        else this.reviewerNames[id] = 'User #' + id;
        this.cdr.detectChanges();
      });
    });
  }

  getReviewerName(r: Review): string {
    return this.reviewerNames[r.reviewerId] ?? 'User #' + r.reviewerId;
  }

  private loadProjectTitles(): void {
    const ids = [...new Set(this.reviews.map((r) => r.projectId))];
    ids.forEach((id) => {
      if (this.projectTitles[id]) return;
      this.projectService.getById(id).subscribe((p) => {
        this.projectTitles[id] = p?.title ?? 'Project #' + id;
        this.cdr.detectChanges();
      });
    });
  }

  getProjectTitle(r: Review): string {
    return this.projectTitles[r.projectId] ?? 'Project #' + r.projectId;
  }

  goToPage(p: number): void {
    if (p < 0 || p >= this.totalPages) return;
    this.page = p;
    this.load();
  }

  setPageSize(newSize: number): void {
    this.size = newSize;
    this.page = 0;
    this.load();
  }

  get roundedAverageRating(): number {
    return this.stats ? Math.round(this.stats.averageRating) : 0;
  }

  get ratingOptions(): { value: number | null; label: string }[] {
    return [
      { value: null, label: 'All ratings' },
      { value: 5, label: '5 stars' },
      { value: 4, label: '4 stars' },
      { value: 3, label: '3 stars' },
      { value: 2, label: '2 stars' },
      { value: 1, label: '1 star' },
    ];
  }
}
