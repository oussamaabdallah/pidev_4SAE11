import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, catchError, map, of } from 'rxjs';
import { environment } from '../../../environments/environment';

const REVIEW_API = `${environment.apiGatewayUrl}/review/api/reviews`;

export interface Review {
  id?: number;
  reviewerId: number;
  revieweeId: number;
  projectId: number;
  rating: number;
  comment?: string | null;
  createdAt?: string;
}

export interface ReviewPageResponse {
  content: Review[];
  totalPages: number;
  totalElements: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

export interface ReviewStats {
  totalCount: number;
  averageRating: number;
  countByRating: Record<number, number>;
}

export interface ReviewPageParams {
  page?: number;
  size?: number;
  search?: string;
  rating?: number | null;
}

@Injectable({ providedIn: 'root' })
export class ReviewService {
  constructor(private http: HttpClient) {}

  private buildPageParams(params: ReviewPageParams): HttpParams {
    let p = new HttpParams();
    if (params.page != null) p = p.set('page', params.page);
    if (params.size != null) p = p.set('size', params.size);
    if (params.search != null && params.search.trim() !== '') p = p.set('search', params.search.trim());
    if (params.rating != null && params.rating >= 1 && params.rating <= 5) p = p.set('rating', params.rating);
    return p;
  }

  getAll(): Observable<Review[]> {
    return this.http.get<Review[]>(REVIEW_API).pipe(
      catchError(() => of([]))
    );
  }

  getPage(params: ReviewPageParams = {}): Observable<ReviewPageResponse | null> {
    const httpParams = this.buildPageParams({ page: 0, size: 10, ...params });
    return this.http.get<ReviewPageResponse>(`${REVIEW_API}/page`, { params: httpParams }).pipe(
      catchError(() => of(null))
    );
  }

  getByReviewerIdPage(reviewerId: number, params: ReviewPageParams = {}): Observable<ReviewPageResponse | null> {
    const httpParams = this.buildPageParams({ page: 0, size: 10, ...params });
    return this.http.get<ReviewPageResponse>(`${REVIEW_API}/reviewer/${reviewerId}/page`, { params: httpParams }).pipe(
      catchError(() => of(null))
    );
  }

  getByRevieweeIdPage(revieweeId: number, params: ReviewPageParams = {}): Observable<ReviewPageResponse | null> {
    const httpParams = this.buildPageParams({ page: 0, size: 10, ...params });
    return this.http.get<ReviewPageResponse>(`${REVIEW_API}/reviewee/${revieweeId}/page`, { params: httpParams }).pipe(
      catchError(() => of(null))
    );
  }

  getStats(): Observable<ReviewStats | null> {
    return this.http.get<ReviewStats>(`${REVIEW_API}/stats`).pipe(
      catchError(() => of(null))
    );
  }

  getStatsByReviewer(reviewerId: number): Observable<ReviewStats | null> {
    return this.http.get<ReviewStats>(`${REVIEW_API}/stats/reviewer/${reviewerId}`).pipe(
      catchError(() => of(null))
    );
  }

  getStatsByReviewee(revieweeId: number): Observable<ReviewStats | null> {
    return this.http.get<ReviewStats>(`${REVIEW_API}/stats/reviewee/${revieweeId}`).pipe(
      catchError(() => of(null))
    );
  }

  getById(id: number): Observable<Review | null> {
    return this.http.get<Review>(`${REVIEW_API}/${id}`).pipe(
      catchError(() => of(null))
    );
  }

  getByReviewerId(reviewerId: number): Observable<Review[]> {
    return this.http.get<Review[]>(`${REVIEW_API}/reviewer/${reviewerId}`).pipe(
      catchError(() => of([]))
    );
  }

  getByRevieweeId(revieweeId: number): Observable<Review[]> {
    return this.http.get<Review[]>(`${REVIEW_API}/reviewee/${revieweeId}`).pipe(
      catchError(() => of([]))
    );
  }

  getByProjectId(projectId: number): Observable<Review[]> {
    return this.http.get<Review[]>(`${REVIEW_API}/project/${projectId}`).pipe(
      catchError(() => of([]))
    );
  }

  getByRevieweeAndProject(revieweeId: number, projectId: number): Observable<Review[]> {
    return this.http.get<Review[]>(`${REVIEW_API}/reviewee/${revieweeId}/project/${projectId}`).pipe(
      catchError(() => of([]))
    );
  }

  create(review: Review): Observable<Review | null> {
    return this.http.post<Review>(REVIEW_API, review).pipe(
      catchError(() => of(null))
    );
  }

  update(id: number, review: Partial<Review>): Observable<Review | null> {
    return this.http.put<Review>(`${REVIEW_API}/${id}`, { ...review, id }).pipe(
      catchError(() => of(null))
    );
  }

  delete(id: number): Observable<boolean> {
    return this.http.delete(`${REVIEW_API}/${id}`, { observe: 'response' }).pipe(
      map((res) => res.status >= 200 && res.status < 300),
      catchError(() => of(false))
    );
  }
}
