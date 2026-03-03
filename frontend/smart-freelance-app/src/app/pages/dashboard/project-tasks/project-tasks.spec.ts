import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { ProjectTasks } from './project-tasks';
import { TaskService } from '../../../core/services/task.service';
import { AuthService } from '../../../core/services/auth.service';
import { ProjectService } from '../../../core/services/project.service';
import { UserService } from '../../../core/services/user.service';
import { of } from 'rxjs';
import { Card } from '../../../shared/components/card/card';

/**
 * Jasmine unit tests for ProjectTasks (client) component. Verifies creation and that init loads
 * client projects via ProjectService and tasks via TaskService. Mocks Auth, Project, User, TaskService.
 */
describe('ProjectTasks', () => {
  let component: ProjectTasks;
  let fixture: ComponentFixture<ProjectTasks>;
  let taskService: jasmine.SpyObj<TaskService>;

  beforeEach(async () => {
    const taskSpy = jasmine.createSpyObj('TaskService', ['getTasksByProjectId']);
    taskSpy.getTasksByProjectId.and.returnValue(of([]));

    const authStub = {
      getUserId: () => 1,
      isClient: () => true,
    };

    await TestBed.configureTestingModule({
      imports: [ProjectTasks, ReactiveFormsModule, Card],
      providers: [
        { provide: TaskService, useValue: taskSpy },
        { provide: AuthService, useValue: authStub },
        { provide: ProjectService, useValue: { getByClientId: () => of([{ id: 1, title: 'Proj 1' }]) } },
        { provide: UserService, useValue: { getAll: () => of([]) } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ProjectTasks);
    component = fixture.componentInstance;
    taskService = TestBed.inject(TaskService) as jasmine.SpyObj<TaskService>;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should call getByClientId on init and load tasks for selected project', () => {
    fixture.detectChanges();
    expect(taskService.getTasksByProjectId).toHaveBeenCalledWith(1);
  });
});
