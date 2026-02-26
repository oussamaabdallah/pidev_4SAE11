import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../../core/services/auth.service';
import { UserService, User } from '../../../core/services/user.service';
import { OfferService, Offer, OfferApplication, OfferQuestionResponse } from '../../../core/services/offer.service';
import { ToastService } from '../../../core/services/toast.service';

@Component({
  selector: 'app-show-offer',
  standalone: true,
  imports: [CommonModule, RouterLink, FormsModule],
  templateUrl: './show-offer.html',
  styleUrl: './show-offer.scss',
})
export class ShowOffer implements OnInit {
  offer: Offer | null = null;
  applications: OfferApplication[] = [];
  loading = true;
  loadingApps = false;
  errorMessage = '';
  actioning = false;
  currentUser: User | null = null;

  selectedTranslateLang: 'fr' | 'en' | 'ar' = 'en';
  translating = false;
  translateError = '';
  translatedResult: { title: string; description: string } | null = null;

  offerQuestions: OfferQuestionResponse[] = [];
  loadingQuestions = false;
  answerTexts: Record<number, string> = {};
  answeringId: number | null = null;

  /** Modal rejet avec raison */
  rejectModalOpen = false;
  rejectApp: OfferApplication | null = null;
  rejectReason = '';
  rejectError: string | null = null;

  constructor(
    private route: ActivatedRoute,
    private auth: AuthService,
    private userService: UserService,
    private offerService: OfferService,
    private toast: ToastService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    const email = this.auth.getPreferredUsername();
    if (!email) {
      this.loading = false;
      this.errorMessage = 'You must be logged in.';
      this.cdr.detectChanges();
      return;
    }
    this.userService.getByEmail(email).subscribe((u) => {
      this.currentUser = u ?? null;
      if (!this.currentUser) {
        this.loading = false;
        this.errorMessage = 'Could not load your profile.';
        this.cdr.detectChanges();
        return;
      }
      this.offerService.getOfferById(id).subscribe((offer) => {
        this.offer = offer ?? null;
        this.loading = false;
        if (this.offer && this.currentUser != null && this.offer.freelancerId === this.currentUser.id) {
          this.loadApplications();
          this.loadQuestions();
        } else if (this.offer) {
          this.errorMessage = 'You do not own this offer.';
        } else {
          this.errorMessage = 'Offer not found.';
        }
        this.cdr.detectChanges();
      });
    });
  }

  loadApplications(): void {
    if (!this.offer?.id || !this.currentUser?.id) return;
    this.loadingApps = true;
    this.offerService.getApplicationsByOffer(this.offer.id, 0, 50).subscribe((page) => {
      this.applications = page.content ?? [];
      this.loadingApps = false;
      this.cdr.detectChanges();
    });
  }

  formatDate(s: string): string {
    if (!s) return '';
    const d = new Date(s);
    return d.toLocaleDateString();
  }

  accept(app: OfferApplication): void {
    if (!this.currentUser?.id) return;
    this.actioning = true;
    this.offerService.acceptApplication(app.id, this.currentUser.id).subscribe({
      next: (res) => {
        this.actioning = false;
        this.loadApplications();
        if (res?.warningMessage) this.toast.warning(res.warningMessage);
        this.cdr.detectChanges();
      },
      error: () => {
        this.actioning = false;
        this.cdr.detectChanges();
      },
    });
  }

  /** Rejet avec raison optionnelle (ouvre le modal). */
  openRejectModal(app: OfferApplication): void {
    this.rejectApp = app;
    this.rejectReason = '';
    this.rejectModalOpen = true;
    this.cdr.detectChanges();
  }

  closeRejectModal(): void {
    this.rejectModalOpen = false;
    this.rejectApp = null;
    this.rejectReason = '';
    this.rejectError = null;
    this.cdr.detectChanges();
  }

  submitReject(): void {
    if (!this.rejectApp || !this.currentUser?.id) return;
    this.actioning = true;
    this.rejectError = null;
    this.offerService.rejectApplication(this.rejectApp.id, this.currentUser.id, this.rejectReason?.trim() || undefined).subscribe({
      next: () => {
        this.actioning = false;
        this.closeRejectModal();
        this.loadApplications();
        this.cdr.detectChanges();
      },
      error: () => {
        this.actioning = false;
        this.rejectError = 'Failed to reject application.';
        this.cdr.detectChanges();
      },
    });
  }

  reject(app: OfferApplication): void {
    this.openRejectModal(app);
  }

  shortlist(app: OfferApplication): void {
    if (!this.currentUser?.id) return;
    this.actioning = true;
    this.offerService.shortlistApplication(app.id, this.currentUser.id).subscribe(() => {
      this.actioning = false;
      this.loadApplications();
      this.cdr.detectChanges();
    });
  }

  markAsRead(app: OfferApplication): void {
    if (!this.currentUser?.id) return;
    this.offerService.markApplicationAsRead(app.id, this.currentUser.id).subscribe(() => {
      this.loadApplications();
      this.cdr.detectChanges();
    });
  }

  runTranslate(): void {
    if (!this.offer?.id) return;
    this.translating = true;
    this.translateError = '';
    this.translatedResult = null;
    this.offerService.translateOffer(this.offer.id, this.selectedTranslateLang).subscribe({
      next: (res) => {
        this.translatedResult = { title: res.title, description: res.description };
        this.translating = false;
        this.cdr.detectChanges();
      },
      error: (err: unknown) => {
        const e = err as { error?: { message?: string }; message?: string };
        this.translateError = e?.error?.message || e?.message || 'Translation failed.';
        this.translating = false;
        this.cdr.detectChanges();
      },
    });
  }

  translateLangLabel(lang: string): string {
    const labels: Record<string, string> = { fr: 'French', en: 'English', ar: 'Arabic' };
    return labels[lang] || lang;
  }

  loadQuestions(): void {
    if (!this.offer?.id) return;
    this.loadingQuestions = true;
    this.offerService.getOfferQuestions(this.offer.id).subscribe((list) => {
      this.offerQuestions = list ?? [];
      this.loadingQuestions = false;
      this.cdr.detectChanges();
    });
  }

  submitAnswer(questionId: number): void {
    if (!this.currentUser?.id) return;
    const text = (this.answerTexts[questionId] || '').trim();
    if (!text) return;
    this.answeringId = questionId;
    this.offerService.answerOfferQuestion(questionId, this.currentUser.id, text).subscribe({
      next: (updated) => {
        this.answeringId = null;
        if (updated) {
          const idx = this.offerQuestions.findIndex((q) => q.id === questionId);
          if (idx >= 0) this.offerQuestions[idx] = updated;
          delete this.answerTexts[questionId];
        }
        this.cdr.detectChanges();
      },
      error: () => {
        this.answeringId = null;
        this.cdr.detectChanges();
      },
    });
  }
}
