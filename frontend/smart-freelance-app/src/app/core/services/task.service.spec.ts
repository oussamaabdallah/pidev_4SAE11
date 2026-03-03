import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { TaskService } from './task.service';

describe('TaskService', () => {
  let service: TaskService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [TaskService],
    });
    service = TestBed.inject(TaskService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('getFilteredTasks should call correct URL', () => {
    service.getFilteredTasks({ page: 0, size: 10 }).subscribe();
    const req = httpMock.expectOne((r) => r.url.includes('/task/api/tasks') && r.url.includes('page=0'));
    expect(req.request.method).toBe('GET');
    req.flush({ content: [], totalElements: 0, totalPages: 0 });
  });

  it('getTaskById should call correct URL', () => {
    service.getTaskById(1).subscribe();
    const req = httpMock.expectOne((r) => r.url.includes('/task/api/tasks/1'));
    expect(req.request.method).toBe('GET');
    req.flush(null);
  });

  it('getTasksByProjectId should call correct URL', () => {
    service.getTasksByProjectId(5).subscribe();
    const req = httpMock.expectOne((r) => r.url.includes('/task/api/tasks/project/5'));
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('createTask should POST', () => {
    service.createTask({ projectId: 1, title: 'Test' }).subscribe();
    const req = httpMock.expectOne((r) => r.url.includes('/task/api/tasks'));
    expect(req.request.method).toBe('POST');
  });
});
