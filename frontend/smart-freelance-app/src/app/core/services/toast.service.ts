import { Injectable, signal, computed } from '@angular/core';

export type ToastType = 'error' | 'success' | 'warning' | 'info';

export interface Toast {
  id: number;
  message: string;
  type: ToastType;
  duration?: number;
  createdAt: number;
}

let nextId = 1;
const DEFAULT_DURATION = 6000;

@Injectable({ providedIn: 'root' })
export class ToastService {
  private toastsSignal = signal<Toast[]>([]);
  readonly toasts = this.toastsSignal.asReadonly();

  /** Affiche une notification toast (erreur, succès, validation, etc.). */
  show(message: string, type: ToastType = 'info', duration: number = DEFAULT_DURATION): number {
    const id = nextId++;
    const toast: Toast = {
      id,
      message,
      type,
      duration: type === 'error' ? Math.max(duration, 8000) : duration,
      createdAt: Date.now(),
    };
    this.toastsSignal.update((list) => [...list, toast]);
    if (toast.duration && toast.duration > 0) {
      setTimeout(() => this.dismiss(id), toast.duration);
    }
    return id;
  }

  success(message: string, duration?: number): number {
    return this.show(message, 'success', duration);
  }

  error(message: string, duration?: number): number {
    return this.show(message, 'error', duration);
  }

  warning(message: string, duration?: number): number {
    return this.show(message, 'warning', duration);
  }

  info(message: string, duration?: number): number {
    return this.show(message, 'info', duration);
  }

  dismiss(id: number): void {
    this.toastsSignal.update((list) => list.filter((t) => t.id !== id));
  }

  dismissAll(): void {
    this.toastsSignal.set([]);
  }
}
