import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { Chart, ChartData, ChartOptions, registerables } from 'chart.js';
import { BaseChartDirective } from 'ng2-charts';
import { TaskService, Task } from '../../../core/services/task.service';
import { AuthService } from '../../../core/services/auth.service';
import { ProjectService, Project } from '../../../core/services/project.service';
import { UserService } from '../../../core/services/user.service';
import { Card } from '../../../shared/components/card/card';

Chart.register(...registerables);

const STATUS_ORDER = ['TODO', 'IN_PROGRESS', 'IN_REVIEW', 'DONE', 'CANCELLED'] as const;
const PRIORITY_ORDER = ['LOW', 'MEDIUM', 'HIGH', 'URGENT'] as const;
const STATUS_COLORS = ['#94a3b8', '#3b82f6', '#f59e0b', '#10b981', '#ef4444'];
const PRIORITY_COLORS = ['#10b981', '#3b82f6', '#f59e0b', '#ef4444'];

@Component({
  selector: 'app-project-tasks',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, Card, BaseChartDirective],
  templateUrl: './project-tasks.html',
  styleUrl: './project-tasks.scss',
})
export class ProjectTasks implements OnInit {
  myProjects: Project[] = [];
  selectedProjectId: number | null = null;
  tasks: Task[] = [];
  loading = true;
  errorMessage = '';
  projectForm: FormGroup;

  statusChartData: ChartData<'doughnut', number[], string> = {
    labels: [...STATUS_ORDER],
    datasets: [{ data: [0, 0, 0, 0, 0], backgroundColor: STATUS_COLORS, borderWidth: 1 }],
  };
  statusChartOptions: ChartOptions<'doughnut'> = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: { legend: { position: 'bottom' } },
  };

  priorityChartData: ChartData<'bar', number[], string> = {
    labels: [...PRIORITY_ORDER],
    datasets: [{ data: [0, 0, 0, 0], label: 'Tasks', backgroundColor: PRIORITY_COLORS }],
  };
  priorityChartOptions: ChartOptions<'bar'> = {
    indexAxis: 'y',
    responsive: true,
    maintainAspectRatio: false,
    plugins: { legend: { display: false } },
    scales: {
      x: { beginAtZero: true, ticks: { stepSize: 1 } },
    },
  };

  constructor(
    private taskService: TaskService,
    public auth: AuthService,
    private projectService: ProjectService,
    private userService: UserService,
    private route: ActivatedRoute,
    private fb: FormBuilder,
    private cdr: ChangeDetectorRef
  ) {
    this.projectForm = this.fb.group({ projectId: [null as number | null] });
  }

  get doneCount(): number {
    return this.tasks.filter((t) => t.status === 'DONE').length;
  }

  get completionPercentage(): number {
    if (this.tasks.length === 0) return 0;
    return (this.doneCount / this.tasks.length) * 100;
  }

  get inProgressCount(): number {
    return this.tasks.filter(
      (t) => t.status === 'IN_PROGRESS' || t.status === 'IN_REVIEW'
    ).length;
  }

  get overdueCount(): number {
    const today = new Date().toISOString().slice(0, 10);
    return this.tasks.filter(
      (t) =>
        t.dueDate &&
        t.dueDate < today &&
        t.status !== 'DONE' &&
        t.status !== 'CANCELLED'
    ).length;
  }

  ngOnInit(): void {
    const userId = this.auth.getUserId();
    if (userId == null) {
      this.loading = false;
      this.cdr.detectChanges();
      return;
    }
    this.projectService.getByClientId(userId).subscribe({
      next: (projects) => {
        this.myProjects = projects ?? [];
        if (this.myProjects.length > 0 && !this.selectedProjectId) {
          const projectIdParam = this.route.snapshot.queryParamMap.get('projectId');
          if (projectIdParam) {
            const id = Number(projectIdParam);
            const found = this.myProjects.some((p) => p.id === id);
            this.selectedProjectId = found ? id : this.myProjects[0].id ?? null;
          } else {
            this.selectedProjectId = this.myProjects[0].id ?? null;
          }
          this.projectForm.patchValue({ projectId: this.selectedProjectId });
          this.loadTasks();
        }
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.loading = false;
        this.cdr.detectChanges();
      },
    });
    this.userService.getAll().subscribe((users) => {
      this.freelancersForAssign = users?.filter((u) => u.role === 'FREELANCER') ?? [];
    });
  }

  onProjectChange(): void {
    this.selectedProjectId = this.projectForm.get('projectId')?.value ?? null;
    this.loadTasks();
  }

  loadTasks(): void {
    if (!this.selectedProjectId) {
      this.tasks = [];
      this.cdr.detectChanges();
      return;
    }
    this.loading = true;
    this.taskService.getTasksByProjectId(this.selectedProjectId).subscribe({
      next: (list) => {
        this.tasks = list ?? [];
        this.updateCharts();
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.errorMessage = 'Failed to load tasks.';
        this.loading = false;
        this.cdr.detectChanges();
      },
    });
  }

  getProjectTitle(p: Project): string {
    return p.title?.trim() || `Project ${p.id}`;
  }

  freelancersForAssign: { id?: number; firstName?: string; lastName?: string }[] = [];

  getAssigneeName(id: number | null): string {
    if (id == null) return '—';
    const u = this.freelancersForAssign.find((f) => f.id === id);
    return u ? `${u.firstName ?? ''} ${u.lastName ?? ''}`.trim() || `User ${id}` : `User ${id}`;
  }

  formatDueDate(d: string | null): string {
    return d ? new Date(d).toLocaleDateString() : '—';
  }

  get isClient(): boolean {
    return this.auth.isClient();
  }

  private updateCharts(): void {
    const statusCounts = STATUS_ORDER.map(
      (s) => this.tasks.filter((t) => t.status === s).length
    );
    this.statusChartData = {
      ...this.statusChartData,
      datasets: [{
        ...this.statusChartData.datasets[0],
        data: statusCounts,
      }],
    };

    const priorityCounts = PRIORITY_ORDER.map(
      (p) => this.tasks.filter((t) => t.priority === p).length
    );
    this.priorityChartData = {
      ...this.priorityChartData,
      datasets: [{
        ...this.priorityChartData.datasets[0],
        data: priorityCounts,
      }],
    };
  }
}
