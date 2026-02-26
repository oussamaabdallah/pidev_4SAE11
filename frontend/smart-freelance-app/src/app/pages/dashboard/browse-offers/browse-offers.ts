import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ReactiveFormsModule, FormBuilder, FormGroup } from '@angular/forms';
import { AuthService } from '../../../core/services/auth.service';
import { UserService, User } from '../../../core/services/user.service';
import { OfferService, Offer, OfferFilterRequest } from '../../../core/services/offer.service';
import { Subscription } from 'rxjs';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';

export const DOMAINS = [
  'Web Development', 'Mobile', 'Design', 'Marketing',
  'Writing', 'Data Science', 'DevOps', 'Other',
];
export const CATEGORIES = [
  'Frontend', 'Backend', 'Full Stack', 'UI/UX',
  'SEO', 'Content', 'Machine Learning', 'Cloud',
];
export const SORT_OPTIONS = [
  { label: 'Newest first',       sortBy: 'createdAt', dir: 'DESC' },
  { label: 'Oldest first',       sortBy: 'createdAt', dir: 'ASC'  },
  { label: 'Price: Low → High',  sortBy: 'price',     dir: 'ASC'  },
  { label: 'Price: High → Low',  sortBy: 'price',     dir: 'DESC' },
  { label: 'Best rated',         sortBy: 'rating',    dir: 'DESC' },
];
export const DURATION_TYPES = ['hourly', 'fixed', 'monthly'];
export const OFFER_STATUSES = [
  { value: 'AVAILABLE',   label: 'Available'   },
  { value: 'IN_PROGRESS', label: 'In Progress' },
  { value: 'COMPLETED',   label: 'Completed'   },
];

@Component({
  selector: 'app-browse-offers',
  standalone: true,
  imports: [CommonModule, RouterLink, ReactiveFormsModule],
  templateUrl: './browse-offers.html',
  styleUrl: './browse-offers.scss',
})
export class BrowseOffers implements OnInit, OnDestroy {
  /* ── State ──────────────────────────────────────────────────────── */
  offers: Offer[] = [];
  recommendedOffers: Offer[] = [];
  loading = false;
  loadingRecommendations = false;
  page = 0;
  readonly size = 12;
  totalElements = 0;
  totalPages = 0;
  currentUser: User | null = null;
  showAdvanced = false;

  /* ── Form ───────────────────────────────────────────────────────── */
  searchForm: FormGroup;

  /* ── Constants (exposed to template) ────────────────────────────── */
  readonly domains       = DOMAINS;
  readonly categories    = CATEGORIES;
  readonly sortOptions   = SORT_OPTIONS;
  readonly durationTypes = DURATION_TYPES;
  readonly offerStatuses = OFFER_STATUSES;

  private formSub: Subscription | null = null;

  constructor(
    private offerService: OfferService,
    public auth: AuthService,
    private userService: UserService,
    private fb: FormBuilder,
    private cdr: ChangeDetectorRef,
  ) {
    this.searchForm = this.fb.group({
      keyword:      [''],
      domain:       [''],
      category:     [''],
      offerStatus:  [''],
      durationType: [''],
      minPrice:     [null],
      maxPrice:     [null],
      minRating:    [null],
      sortIndex:    [0],
    });
  }

  ngOnInit(): void {
    /* Charger l'utilisateur → recommandations */
    const email = this.auth.getPreferredUsername();
    if (email) {
      this.userService.getByEmail(email).subscribe((u) => {
        this.currentUser = u ?? null;
        if (this.currentUser?.id) this.loadRecommendations();
        this.cdr.detectChanges();
      });
    }

    /*
     * AJAX auto-search : chaque modification du formulaire déclenche
     * un appel HTTP après 350 ms (debounce) pour éviter les appels
     * inutiles pendant la saisie.
     */
    this.formSub = this.searchForm.valueChanges
      .pipe(
        debounceTime(350),
        distinctUntilChanged(
          (a, b) => JSON.stringify(a) === JSON.stringify(b),
        ),
      )
      .subscribe(() => {
        this.page = 0;   // reset page à chaque nouveau filtre
        this.search();
      });

    /* Chargement initial */
    this.search();
  }

  ngOnDestroy(): void {
    this.formSub?.unsubscribe();
  }

  /* ── Helpers ────────────────────────────────────────────────────── */

  get hasActiveFilters(): boolean {
    const v = this.searchForm.value;
    return !!(
      v.keyword     || v.domain       || v.category ||
      v.offerStatus || v.durationType ||
      v.minPrice != null || v.maxPrice != null || v.minRating != null ||
      Number(v.sortIndex) !== 0
    );
  }

  toggleAdvanced(): void {
    this.showAdvanced = !this.showAdvanced;
  }

  resetFilters(): void {
    /* patchValue évite de déclencher valueChanges deux fois */
    this.searchForm.patchValue({
      keyword: '', domain: '', category: '', offerStatus: '',
      durationType: '', minPrice: null, maxPrice: null,
      minRating: null, sortIndex: 0,
    });
    this.page = 0;
    this.search();
  }

  /* ── AJAX : construit le filtre et appelle le backend ───────────── */

  private buildFilter(): OfferFilterRequest {
    const v   = this.searchForm.value;
    const idx = Number(v.sortIndex ?? 0);
    const sort = SORT_OPTIONS[idx] ?? SORT_OPTIONS[0];

    return {
      keyword:       v.keyword?.trim()  || undefined,
      domain:        v.domain           || undefined,
      category:      v.category         || undefined,
      offerStatus:   v.offerStatus      || undefined,
      durationType:  v.durationType     || undefined,
      minPrice:      v.minPrice  != null ? Number(v.minPrice)  : undefined,
      maxPrice:      v.maxPrice  != null ? Number(v.maxPrice)  : undefined,
      minRating:     v.minRating != null ? Number(v.minRating) : undefined,
      page:          this.page,
      size:          this.size,
      sortBy:        sort.sortBy,
      sortDirection: sort.dir,
    };
  }

  /**
   * Envoie une requête AJAX POST /offer/api/offers/search
   * ou GET /offer/api/offers si aucun filtre actif.
   */
  search(): void {
    this.loading = true;
    this.cdr.detectChanges();

    const filter = this.buildFilter();

    /* Utilise l'endpoint de recherche avancée uniquement si filtre actif */
    const request$ = this.hasActiveFilters
      ? this.offerService.searchOffers(filter)          // POST /search (AJAX)
      : this.offerService.getActiveOffers(this.page, this.size); // GET /offers

    request$.subscribe({
      next: (page) => {
        this.offers        = page.content       ?? [];
        this.totalElements = page.totalElements ?? 0;
        this.totalPages    = page.totalPages    ?? 0;
        this.loading       = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.loading = false;
        this.cdr.detectChanges();
      },
    });
  }

  /* ── Recommandations ─────────────────────────────────────────────── */

  loadRecommendations(): void {
    if (!this.currentUser?.id) return;
    this.loadingRecommendations = true;
    this.offerService.getRecommendedOffers(this.currentUser.id, 8).subscribe((list) => {
      this.recommendedOffers = list ?? [];
      this.loadingRecommendations = false;
      this.cdr.detectChanges();
    });
  }

  /* ── Pagination ──────────────────────────────────────────────────── */

  prev(): void { if (this.page > 0)                       { this.page--; this.search(); } }
  next(): void { if (this.page < this.totalPages - 1)     { this.page++; this.search(); } }
  goToPage(p: number): void {
    if (p >= 0 && p < this.totalPages) { this.page = p; this.search(); }
  }

  get pages(): number[] {
    const total = Math.min(this.totalPages, 7);
    const start = Math.max(0, Math.min(this.page - 3, this.totalPages - total));
    return Array.from({ length: total }, (_, i) => start + i);
  }
}
