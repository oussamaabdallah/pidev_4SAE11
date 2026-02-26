import { inject } from '@angular/core';
import { Router, CanActivateFn } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const authGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  if (auth.isLoggedIn()) {
    return true;
  }
  // Si le token est en localStorage mais pas dans le signal (race / désync), resynchroniser et autoriser
  if (auth.hasStoredToken()) {
    auth.syncTokenFromStorage();
    return true;
  }
  router.navigate(['/login']);
  return false;
};
