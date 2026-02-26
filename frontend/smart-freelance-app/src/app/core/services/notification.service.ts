import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, map, of } from 'rxjs';
import { environment } from '../../../environments/environment';

const NOTIFICATION_API = `${environment.apiGatewayUrl}/offer/api/notifications`;

export type NotificationType = 'NEW_QUESTION' | 'QUESTION_ANSWERED';

export interface AppNotification {
  id: number;
  type: NotificationType;
  title: string;
  message: string;
  offerId: number | null;
  questionId: number | null;
  read: boolean;
  createdAt: string;
}

@Injectable({ providedIn: 'root' })
export class NotificationService {
  constructor(private http: HttpClient) {}

  getNotifications(recipientUserId: number, limit = 50): Observable<AppNotification[]> {
    return this.http
      .get<AppNotification[]>(NOTIFICATION_API, {
        params: { recipientUserId: String(recipientUserId), limit: String(limit) },
      })
      .pipe(catchError(() => of([])));
  }

  getUnreadCount(recipientUserId: number): Observable<number> {
    return this.http
      .get<{ count: number }>(`${NOTIFICATION_API}/unread-count`, {
        params: { recipientUserId: String(recipientUserId) },
      })
      .pipe(
        map((r: { count: number }) => r?.count ?? 0),
        catchError(() => of(0))
      );
  }

  markAsRead(notificationId: number, userId: number): Observable<void> {
    return this.http
      .patch<void>(`${NOTIFICATION_API}/${notificationId}/read`, null, {
        params: { userId: String(userId) },
      })
      .pipe(catchError(() => of(undefined)));
  }
}
