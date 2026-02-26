import { HttpInterceptorFn, HttpErrorResponse, HttpRequest } from '@angular/common/http';
import { catchError, throwError } from 'rxjs';

/** Si présent, un 401 ne déclenche pas le logout (conservé pour usage futur si on réactive le logout ciblé). */
export const SKIP_UNAUTHORIZED_LOGOUT_HEADER = 'X-Skip-Unauthorized-Logout';

/**
 * Ne plus déclencher de logout automatique sur 401.
 * Les 401 (gateway, microservices, token non transmis) provoquaient des déconnexions intempestives
 * sur les pages client (Apply, My Applications, etc.). L'utilisateur reste connecté ; en cas de
 * session vraiment expirée, il peut se déconnecter manuellement ou le message d'erreur s'affiche.
 */
export const unauthorizedInterceptor: HttpInterceptorFn = (req, next) => {
  const skipLogout = req.headers.has(SKIP_UNAUTHORIZED_LOGOUT_HEADER);
  const reqToSend: HttpRequest<unknown> = skipLogout
    ? req.clone({ headers: req.headers.delete(SKIP_UNAUTHORIZED_LOGOUT_HEADER) })
    : req;

  return next(reqToSend).pipe(
    catchError((err: HttpErrorResponse) => {
      // Désactivé : plus de logout auto sur 401 pour éviter les déconnexions côté client
      // if (err?.status === 401 && !skipLogout) auth.logout();
      return throwError(() => err);
    })
  );
};
