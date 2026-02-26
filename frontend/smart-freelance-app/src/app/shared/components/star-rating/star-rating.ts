import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-star-rating',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './star-rating.html',
  styleUrl: './star-rating.scss',
})
export class StarRating {
  @Input() rating = 0;
  @Input() maxStars = 5;
  @Input() showValue = false;

  get stars(): number[] {
    return Array.from({ length: this.maxStars }, (_, i) => i + 1);
  }

  filled(i: number): boolean {
    return i <= this.rating;
  }
}
