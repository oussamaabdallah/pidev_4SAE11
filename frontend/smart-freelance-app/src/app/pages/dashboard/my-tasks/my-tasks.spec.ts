import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { MyTasks } from './my-tasks';
import { TaskService } from '../../../core/services/task.service';
import { AuthService } from '../../../core/services/auth.service';
import { ProjectService } from '../../../core/services/project.service';
import { of } from 'rxjs';
import { Card } from '../../../shared/components/card/card';

/**
 * Jasmine unit tests for MyTasks (freelancer) component. Verifies creation and that init loads
 * tasks by assignee via TaskService. Mocks Auth, Project, TaskService.
 */
describe('MyTasks', () => {
  let component: MyTasks;
  let fixture: ComponentFixture<MyTasks>;
  let taskService: jasmine.SpyObj<TaskService>;

  beforeEach(async () => {
    const taskSpy = jasmine.createSpyObj('TaskService', ['getTasksByAssigneeId', 'patchStatus', 'createTask', 'updateTask', 'deleteTask']);
    taskSpy.getTasksByAssigneeId.and.returnValue(of([]));

    const authStub = {
      getUserId: () => 1,
      isFreelancer: () => true,
    };

    const projectStub = {
      getAllProjects: () => of([]),
      getByFreelancerId: () => of([]),
    };

    await TestBed.configureTestingModule({
      imports: [MyTasks, ReactiveFormsModule, Card],
      providers: [
        { provide: TaskService, useValue: taskSpy },
        { provide: AuthService, useValue: authStub },
        { provide: ProjectService, useValue: projectStub },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(MyTasks);
    component = fixture.componentInstance;
    taskService = TestBed.inject(TaskService) as jasmine.SpyObj<TaskService>;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should call getTasksByAssigneeId on init', () => {
    fixture.detectChanges();
    expect(taskService.getTasksByAssigneeId).toHaveBeenCalledWith(1);
  });
});
