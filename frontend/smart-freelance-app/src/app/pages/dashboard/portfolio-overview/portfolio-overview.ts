import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subscription } from 'rxjs';
import { AuthService } from '../../../core/services/auth.service';
import { PortfolioService, Experience, Skill } from '../../../core/services/portfolio.service';
import { Card } from '../../../shared/components/card/card';
import { SkillManagement } from '../skill-management/skill-management';

@Component({
  selector: 'app-portfolio-overview',
  imports: [CommonModule, FormsModule, Card, SkillManagement],
  templateUrl: './portfolio-overview.html',
  styleUrl: './portfolio-overview.scss',
  standalone: true,
})
export class PortfolioOverview implements OnInit, OnDestroy {
  experiences: Experience[] = [];
  skills: Skill[] = [];

  // Form State
  showForm = false;
  isEditing = false;
  currentExperience!: Experience;

  // Helpers for form inputs
  newKeyTask = '';
  newSkillName = '';

  // Subscription management
  private skillsSubscription?: Subscription;

  constructor(
    public auth: AuthService,
    private portfolioService: PortfolioService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit() {
    this.currentExperience = this.getEmptyExperience();
    console.log('PortfolioOverview initialized');

    // Load initial data
    this.loadExperiences();
    this.loadSkills();

    // Subscribe to skills updates (triggered when navigating back from skill management)
    this.skillsSubscription = this.portfolioService.skillsUpdated$.subscribe(() => {
      console.log('Skills updated notification received, refreshing skills...');
      // Use setTimeout to avoid ExpressionChangedAfterItHasBeenCheckedError
      setTimeout(() => {
        this.loadSkills();
      }, 0);
    });
  }

  ngOnDestroy() {
    // Clean up subscription to prevent memory leaks
    this.skillsSubscription?.unsubscribe();
  }

  getEmptyExperience(): Experience {
    return {
      userId: this.auth.getUserId() || 1,
      title: '',
      type: 'JOB',
      description: '',
      startDate: '',
      endDate: '',
      companyOrClientName: '',
      keyTasks: [],
      skillNames: []
    };
  }

  loadExperiences() {
    const userId = this.auth.getUserId() || 1;
    console.log('Loading experiences for user:', userId);
    this.portfolioService.getExperiences(userId).subscribe({
      next: (exps) => {
        console.log('Experiences loaded:', exps);
        this.experiences = exps;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Error loading experiences:', err);
      }
    });
  }

  loadSkills() {
    const userId = this.auth.getUserId() || 1;
    console.log('Loading skills for user:', userId);
    this.portfolioService.getUserSkills(userId).subscribe({
      next: (skills) => {
        console.log('Skills loaded in portfolio:', skills);
        this.skills = skills;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Error loading skills:', err);
      }
    });
  }

  openAddForm() {
    this.currentExperience = this.getEmptyExperience();
    this.isEditing = false;
    this.showForm = true;
  }

  openEditForm(exp: Experience) {
    this.currentExperience = { ...exp, skillNames: [] }; // Reset skillNames for editing or fetch if needed
    // Note: Backend returns 'skills' (objects) but form uses 'skillNames' (strings) for adding new ones.
    // Ideally we map existing skills to skillNames if we want to show them in the tag input.
    if(exp.skills) {
      this.currentExperience.skillNames = exp.skills.map(s => s.name);
    }
    this.isEditing = true;
    this.showForm = true;
  }

  deleteExperience(id: number) {
    if(confirm('Delete this experience?')) {
      this.portfolioService.deleteExperience(id).subscribe(() => this.loadExperiences());
    }
  }

  private isValidDate(value: string): boolean {
    if (!value) return true; // empty is allowed (e.g. endDate)
    const iso = /^\d{4}-\d{2}-\d{2}$/.test(value);
    if (!iso) return false;
    const d = new Date(value);
    return !isNaN(d.getTime());
  }

  saveExperience() {
    if (!this.isValidDate(this.currentExperience.startDate)) {
      alert('Start date is invalid. Please use the date picker or enter a valid date (YYYY-MM-DD).');
      return;
    }
    if (!this.isValidDate(this.currentExperience.endDate)) {
      alert('End date is invalid. Please use the date picker or enter a valid date (YYYY-MM-DD).');
      return;
    }

    if (this.isEditing && this.currentExperience.id) {
      this.portfolioService.updateExperience(this.currentExperience.id, this.currentExperience).subscribe(() => {
        this.loadExperiences();
        this.portfolioService.notifySkillsUpdated();
        this.showForm = false;
      });
    } else {
      this.portfolioService.createExperience(this.currentExperience).subscribe(() => {
        this.loadExperiences();
        this.portfolioService.notifySkillsUpdated();
        this.showForm = false;
      });
    }
  }


  addKeyTask() {
    if (this.newKeyTask) {
      this.currentExperience.keyTasks.push(this.newKeyTask);
      this.newKeyTask = '';
    }
  }

  removeKeyTask(index: number) {
    this.currentExperience.keyTasks.splice(index, 1);
  }

  addSkillTag() {
    if (this.newSkillName) {
      if (!this.currentExperience.skillNames) this.currentExperience.skillNames = [];
      this.currentExperience.skillNames.push(this.newSkillName);
      this.newSkillName = '';
    }
  }

  removeSkillTag(index: number) {
    this.currentExperience.skillNames?.splice(index, 1);
  }
}
