import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../../core/services/auth.service';
import { UserService, User } from '../../../core/services/user.service';
import { OfferService, Offer, OfferApplication, OfferApplicationRequest } from '../../../core/services/offer.service';
import { ContractService } from '../../../core/services/contract.service';
import { ToastService } from '../../../core/services/toast.service';

@Component({
  selector: 'app-my-offer-applications',
  standalone: true,
  imports: [CommonModule, RouterLink, FormsModule],
  templateUrl: './my-offer-applications.html',
  styleUrl: './my-offer-applications.scss',
})
export class MyOfferApplications implements OnInit {
  applications: OfferApplication[] = [];
  /** applicationId -> contractId pour les candidatures acceptées (accès au contrat/projet) */
  contractIdByApplicationId: Record<number, number> = {};
  loading = false;
  currentUser: User | null = null;

  /** Offres recommandées pour postuler (section "Recommended for you") */
  recommendedOffers: Offer[] = [];
  loadingRecommendations = false;

  /** Withdraw / Delete en cours (id de l'application) */
  actioningId: number | null = null;

  /** Modal édition : application en cours d'édition */
  editModalOpen = false;
  editApp: OfferApplication | null = null;
  editForm: { message: string; proposedBudget: number | null; portfolioUrl: string; estimatedDuration: number | null } = {
    message: '',
    proposedBudget: null,
    portfolioUrl: '',
    estimatedDuration: null,
  };
  submittingEdit = false;
  editError: string | null = null;

  constructor(
    private auth: AuthService,
    private userService: UserService,
    private offerService: OfferService,
    private contractService: ContractService,
    private toast: ToastService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    const email = this.auth.getPreferredUsername();
    if (!email) {
      this.cdr.detectChanges();
      return;
    }
    this.userService.getByEmail(email).subscribe((u) => {
      this.currentUser = u ?? null;
      if (this.currentUser) {
        this.load();
        this.loadContractsForClient();
        this.loadRecommendedOffers();
      }
      this.cdr.detectChanges();
    });
  }

  /** Charge les offres recommandées pour permettre de postuler depuis cette page. */
  loadRecommendedOffers(): void {
    const clientId = this.currentUser?.id ?? this.auth.getUserId();
    if (!clientId) return;
    this.loadingRecommendations = true;
    this.offerService.getRecommendedOffers(clientId, 10).subscribe({
      next: (list) => {
        this.recommendedOffers = list ?? [];
        this.loadingRecommendations = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.loadingRecommendations = false;
        this.cdr.detectChanges();
      },
    });
  }

  load(): void {
    if (!this.currentUser?.id) return;
    this.loading = true;
    this.offerService.getApplicationsByClient(this.currentUser.id, 0, 100).subscribe((page) => {
      this.applications = page.content ?? [];
      this.loading = false;
      this.cdr.detectChanges();
    });
  }

  /** Charge les contrats du client pour lier les candidatures acceptées au contrat (accès projet). */
  loadContractsForClient(): void {
    const clientId = this.currentUser?.id ?? this.auth.getUserId();
    if (!clientId) return;
    this.contractService.getByClient(clientId).subscribe({
      next: (contracts) => {
        this.contractIdByApplicationId = {};
        (contracts ?? []).forEach((c) => {
          if (c.id != null && c.offerApplicationId != null) {
            this.contractIdByApplicationId[c.offerApplicationId] = c.id;
          }
        });
        this.cdr.detectChanges();
      },
      error: () => {},
    });
  }

  getContractIdForApplication(app: OfferApplication): number | null {
    return app.status === 'ACCEPTED' ? this.contractIdByApplicationId[app.id] ?? null : null;
  }

  formatDate(s: string): string {
    if (!s) return '';
    return new Date(s).toLocaleDateString();
  }

  /** Retirer sa candidature (PENDING / SHORTLISTED). */
  withdraw(app: OfferApplication): void {
    if (!this.currentUser?.id || this.actioningId != null) return;
    this.actioningId = app.id;
    this.offerService.withdrawApplication(app.id, this.currentUser.id).subscribe({
      next: () => {
        this.actioningId = null;
        this.toast.success('Application withdrawn.');
        this.load();
        this.cdr.detectChanges();
      },
      error: () => {
        this.actioningId = null;
        this.toast.error('Failed to withdraw application.');
        this.cdr.detectChanges();
      },
    });
  }

  /** Supprimer définitivement (client). */
  deleteApp(app: OfferApplication): void {
    if (!this.currentUser?.id || this.actioningId != null) return;
    if (!confirm('Delete this application permanently?')) return;
    this.actioningId = app.id;
    this.offerService.deleteApplication(app.id, this.currentUser.id).subscribe({
      next: (ok) => {
        this.actioningId = null;
        if (ok) {
          this.toast.success('Application deleted.');
          this.load();
        } else {
          this.toast.error('Failed to delete application.');
        }
        this.cdr.detectChanges();
      },
      error: () => {
        this.actioningId = null;
        this.toast.error('Failed to delete application.');
        this.cdr.detectChanges();
      },
    });
  }

  /** Ouvrir le modal d'édition (PENDING uniquement si modifiable). */
  openEditModal(app: OfferApplication): void {
    this.editApp = app;
    this.editForm = {
      message: app.message ?? '',
      proposedBudget: app.proposedBudget ?? null,
      portfolioUrl: app.portfolioUrl ?? '',
      estimatedDuration: app.estimatedDuration ?? null,
    };
    this.editError = null;
    this.editModalOpen = true;
    this.cdr.detectChanges();
  }

  closeEditModal(): void {
    this.editModalOpen = false;
    this.editApp = null;
    this.editError = null;
    this.cdr.detectChanges();
  }

  submitEdit(): void {
    if (!this.editApp || !this.currentUser?.id) return;
    const msg = (this.editForm.message || '').trim();
    if (msg.length < 20) {
      this.editError = 'Message must be at least 20 characters.';
      this.cdr.detectChanges();
      return;
    }
    if (this.editForm.proposedBudget == null || this.editForm.proposedBudget < 0.01) {
      this.editError = 'Please enter a valid budget.';
      this.cdr.detectChanges();
      return;
    }
    this.submittingEdit = true;
    this.editError = null;
    const request: OfferApplicationRequest = {
      offerId: this.editApp.offerId,
      clientId: this.currentUser.id,
      message: msg,
      proposedBudget: this.editForm.proposedBudget,
      portfolioUrl: this.editForm.portfolioUrl || undefined,
      estimatedDuration: this.editForm.estimatedDuration ?? undefined,
    };
    this.offerService.updateApplication(this.editApp.id, request).subscribe({
      next: (updated) => {
        this.submittingEdit = false;
        if (updated) {
          this.toast.success('Application updated.');
          this.closeEditModal();
          this.load();
        } else {
          this.editError = 'Update failed.';
        }
        this.cdr.detectChanges();
      },
      error: () => {
        this.submittingEdit = false;
        this.editError = 'Update failed. Please try again.';
        this.cdr.detectChanges();
      },
    });
  }

  /** Peut-on retirer ou modifier cette candidature ? */
  canWithdrawOrEdit(app: OfferApplication): boolean {
    return app.status === 'PENDING' || app.status === 'SHORTLISTED';
  }
}
