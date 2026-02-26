import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../../core/services/auth.service';
import { UserService, User } from '../../../core/services/user.service';
import {
  OfferService,
  Offer,
  OfferApplicationRequest,
  OfferQuestionResponse,
} from '../../../core/services/offer.service';
import { ToastService } from '../../../core/services/toast.service';

@Component({
  selector: 'app-offer-detail',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule, RouterLink],
  templateUrl: './offer-detail.html',
  styleUrl: './offer-detail.scss',
})
export class OfferDetail implements OnInit {
  offer: Offer | null = null;
  loading = true;
  applyModalOpen = false;
  form: FormGroup;
  submitting = false;
  submitError: string | null = null;
  currentUser: User | null = null;

  questions: OfferQuestionResponse[] = [];
  loadingQuestions = false;
  questionText = '';
  submittingQuestion = false;
  questionError: string | null = null;

  /** Freelancer (vendeur) pour le bloc "from ..." dans le modal Request to order */
  freelancer: User | null = null;

  /** Packages & extras (pour le flux professionnel) */
  selectedPackage: 'BASIC' | 'STANDARD' | 'PREMIUM' | null = null;
  selectedExtras: { name: string; price: number }[] = [];
  offerExtras: { name: string; price: number }[] = [];

  constructor(
    private route: ActivatedRoute,
    private auth: AuthService,
    private userService: UserService,
    private offerService: OfferService,
    private toast: ToastService,
    private fb: FormBuilder,
    private cdr: ChangeDetectorRef
  ) {
    this.form = this.fb.group({
      message: ['', [Validators.required, Validators.minLength(20), Validators.maxLength(2000)]],
      proposedBudget: [null, [Validators.required, Validators.min(0.01)]],
      estimatedDuration: [null, [Validators.min(1)]],
      portfolioUrl: [''],
    });
  }

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    const email = this.auth.getPreferredUsername();
    const loadOffer = (userId: number | null) => {
      this.offerService.getOfferById(id).subscribe((offer) => {
        this.offer = offer ?? null;
        this.loading = false;
        if (userId && offer?.id) {
          this.offerService.recordOfferView(userId, offer.id).subscribe();
        }
        if (offer?.id) {
          this.loadQuestions(offer.id);
          this.loadFreelancer(offer.freelancerId);
        }
        this.cdr.detectChanges();
      });
    };
    if (!email) {
      this.currentUser = null;
      loadOffer(null);
      return;
    }
    this.userService.getByEmail(email).subscribe((u) => {
      this.currentUser = u ?? null;
      loadOffer(this.currentUser?.id ?? null);
    });
  }

  loadQuestions(offerId: number): void {
    this.loadingQuestions = true;
    this.offerService.getOfferQuestions(offerId).subscribe((list) => {
      this.questions = list ?? [];
      this.loadingQuestions = false;
      this.cdr.detectChanges();
    });
  }

  loadFreelancer(freelancerId: number): void {
    this.userService.getById(freelancerId).subscribe((u) => {
      this.freelancer = u ?? null;
      this.cdr.detectChanges();
    });
  }

  scrollToQa(): void {
    document.getElementById('qa-section')?.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }

  getFreelancerAvatar(): string {
    if (this.freelancer?.avatarUrl?.trim()) return this.freelancer.avatarUrl.trim();
    const name = this.freelancer
      ? (`${this.freelancer.firstName ?? ''} ${this.freelancer.lastName ?? ''}`.trim() || 'F')
      : 'F';
    return `https://ui-avatars.com/api/?name=${encodeURIComponent(name)}&background=dddddd&color=666`;
  }

  onSellerAvatarError(e: Event): void {
    const el = e.target as HTMLImageElement;
    if (el && this.freelancer) {
      const name = (`${this.freelancer.firstName ?? ''} ${this.freelancer.lastName ?? ''}`.trim() || 'F');
      el.src = `https://ui-avatars.com/api/?name=${encodeURIComponent(name)}&background=dddddd&color=666`;
    }
  }

  submitQuestion(): void {
    if (!this.offer?.id || !this.currentUser?.id || !this.questionText?.trim() || this.questionText.trim().length < 10) return;
    this.submittingQuestion = true;
    this.questionError = null;
    this.offerService.addOfferQuestion(this.offer.id, this.currentUser.id, this.questionText.trim()).subscribe({
      next: (q) => {
        this.submittingQuestion = false;
        this.questions = [q, ...this.questions];
        this.questionText = '';
        this.cdr.detectChanges();
      },
      error: () => {
        this.submittingQuestion = false;
        this.questionError = 'Failed to send question.';
        this.toast.error('Failed to send question.');
        this.cdr.detectChanges();
      },
    });
  }

  formatDate(s: string | null | undefined): string {
    if (!s) return '';
    const d = new Date(s);
    return d.toLocaleDateString(undefined, { dateStyle: 'short' });
  }

  get hasPackages(): boolean {
    const o = this.offer;
    return !!(o && (o.basicPrice != null || o.standardPrice != null || o.premiumPrice != null));
  }

  get packagePrice(): number | null {
    const o = this.offer;
    if (!o || !this.selectedPackage) return null;
    const v = this.selectedPackage === 'BASIC' ? o.basicPrice : this.selectedPackage === 'STANDARD' ? o.standardPrice : o.premiumPrice;
    return v != null ? Number(v) : null;
  }

  get totalAmount(): number {
    const p = this.packagePrice;
    const base = p != null ? p : (this.offer?.price != null ? Number(this.offer.price) : 0);
    const extrasSum = this.selectedExtras.reduce((s, e) => s + e.price, 0);
    return base + extrasSum;
  }

  get hasExtras(): boolean {
    return this.offerExtras.length > 0;
  }

  toggleExtra(extra: { name: string; price: number }): void {
    const i = this.selectedExtras.findIndex((e) => e.name === extra.name && e.price === extra.price);
    if (i >= 0) this.selectedExtras = this.selectedExtras.filter((_, idx) => idx !== i);
    else this.selectedExtras = [...this.selectedExtras, extra];
    this.updateProposedBudgetFromTotal();
  }

  isExtraSelected(extra: { name: string; price: number }): boolean {
    return this.selectedExtras.some((e) => e.name === extra.name && e.price === extra.price);
  }

  private updateProposedBudgetFromTotal(): void {
    const total = this.totalAmount;
    if (total > 0) this.form.patchValue({ proposedBudget: total });
  }

  openApplyModal(): void {
    this.submitError = null;
    this.selectedPackage = null;
    this.selectedExtras = [];
    this.offerExtras = [];
    if (this.offer?.extrasJson) {
      try {
        this.offerExtras = JSON.parse(this.offer.extrasJson) as { name: string; price: number }[];
        if (!Array.isArray(this.offerExtras)) this.offerExtras = [];
      } catch {
        this.offerExtras = [];
      }
    }
    if (this.hasPackages && this.offer) {
      if (this.offer.basicPrice != null) this.selectedPackage = 'BASIC';
      else if (this.offer.standardPrice != null) this.selectedPackage = 'STANDARD';
      else if (this.offer.premiumPrice != null) this.selectedPackage = 'PREMIUM';
    }
    const total = this.totalAmount;
    this.form.reset({
      message: '',
      proposedBudget: total > 0 ? total : null,
      estimatedDuration: null,
      portfolioUrl: '',
    });
    this.applyModalOpen = true;
  }

  closeApplyModal(): void {
    if (!this.submitting) this.applyModalOpen = false;
  }

  selectPackage(pkg: 'BASIC' | 'STANDARD' | 'PREMIUM'): void {
    this.selectedPackage = pkg;
    this.updateProposedBudgetFromTotal();
  }

  submitApplication(): void {
    if (!this.offer || !this.currentUser?.id || this.form.invalid) return;
    this.form.markAllAsTouched();
    const total = this.totalAmount;
    const budget = Number(this.form.value.proposedBudget);
    if (this.hasPackages && total > 0 && budget <= 0) {
      this.form.patchValue({ proposedBudget: total });
    }
    if (this.form.invalid) return;
    this.submitting = true;
    this.submitError = null;
    const v = this.form.value;
    const proposedBudget = budget > 0 ? budget : total;
    const req: OfferApplicationRequest = {
      offerId: this.offer.id,
      clientId: this.currentUser.id,
      message: (v.message as string).trim(),
      proposedBudget,
      estimatedDuration: v.estimatedDuration ? Number(v.estimatedDuration) : undefined,
      portfolioUrl: (v.portfolioUrl as string)?.trim() || undefined,
      selectedPackage: this.selectedPackage ?? undefined,
      selectedExtrasJson: this.selectedExtras.length > 0 ? JSON.stringify(this.selectedExtras) : undefined,
      totalAmount: total > 0 ? total : undefined,
    };
    this.offerService.applyToOffer(req).subscribe({
      next: () => {
        this.submitting = false;
        this.applyModalOpen = false;
        this.toast.success('Request sent! The freelancer will get back to you.');
        this.cdr.detectChanges();
      },
      error: (err: unknown) => {
        this.submitting = false;
        const e = err as { error?: { message?: string }; message?: string };
        this.submitError = e?.error?.message || e?.message || 'Impossible d\'envoyer la candidature. Veuillez réessayer.';
        this.cdr.detectChanges();
      },
    });
  }
}
