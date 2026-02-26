import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ToastService, Toast, ToastType } from '../../../core/services/toast.service';

@Component({
  selector: 'app-toast',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './toast.html',
  styleUrl: './toast.scss',
})
export class ToastComponent {
  constructor(public toast: ToastService) {}

  iconFor(type: ToastType): string {
    switch (type) {
      case 'error':
        return '✕';
      case 'success':
        return '✓';
      case 'warning':
        return '⚠';
      default:
        return 'ℹ';
    }
  }

  close(id: number): void {
    this.toast.dismiss(id);
  }
}
