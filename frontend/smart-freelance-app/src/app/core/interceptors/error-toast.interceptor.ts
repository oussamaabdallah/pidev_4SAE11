import { inject } from '@angular/core';
import { HttpInterceptorFn, HttpErrorResponse, HttpRequest } from '@angular/common/http';
import { catchError, throwError } from 'rxjs';
import { ToastService } from '../services/toast.service';

/** Header pour ne pas afficher le toast générique (le composant affichera son propre message). */
export const SKIP_ERROR_TOAST_HEADER = 'X-Skip-Error-Toast';

/** Messages d'erreur de programme / technique : ne jamais les afficher à l'utilisateur. */
function isTechnicalMessage(msg: string): boolean {
  if (!msg || typeof msg !== 'string') return true;
  const lower = msg.toLowerCase();
  if (msg.length > 250) return true;
  return (
    lower.includes('jdbc') ||
    lower.includes('sql ') ||
    lower.includes('table ') ||
    lower.includes("doesn't exist") ||
    lower.includes('exception') ||
    lower.includes('constraint') ||
    lower.includes('syntax') ||
    lower.includes('java.') ||
    lower.includes('jakarta.') ||
    lower.includes('org.springframework') ||
    lower.includes('at com.') ||
    lower.includes('at org.') ||
    lower.includes('at java.') ||
    lower.includes('caused by') ||
    lower.includes('nullpointer') ||
    lower.includes('stacktrace') ||
    lower.includes('nested exception') ||
    /\bexception\b/.test(lower) ||
    /\.(getMessage|toString)\(\)/.test(msg)
  );
}

const GENERIC_ERROR = 'Une erreur est survenue. Veuillez réessayer.';

function extractMessage(err: HttpErrorResponse): string {
  // Erreurs 5xx : jamais afficher le détail serveur, uniquement le message générique.
  if (err?.status && err.status >= 500) return GENERIC_ERROR;

  const body = err?.error;
  let message: string | null = null;

  if (typeof body === 'string' && body.length > 0) {
    message = body;
  } else if (body && typeof body === 'object') {
    if (body.message && typeof body.message === 'string') message = body.message;
    else if (body.error) message = typeof body.error === 'string' ? body.error : (body.error?.message || body.error);
    else if (err.status === 400) {
      const msg = body.message;
      if (typeof msg === 'string') message = msg;
      else {
        const values = Object.values(body).filter((v): v is string => typeof v === 'string');
        if (values.length) message = values.join('. ');
      }
    }
  }
  if (message && isTechnicalMessage(message)) message = null;
  if (message) return message;
  if (err?.message && !isTechnicalMessage(err.message)) return err.message;
  if (err?.status === 0) return 'Erreur réseau. Vérifiez votre connexion.';
  if (err?.status === 401) return 'Session expirée ou invalide. Veuillez vous reconnecter.';
  if (err?.status === 403) return "Vous n'avez pas la permission pour cette action.";
  if (err?.status === 404) return 'Ressource introuvable.';
  return GENERIC_ERROR;
}

/**
 * Affiche un toast d'erreur pour toute réponse HTTP 4xx/5xx (sauf 401).
 * Si la requête a le header X-Skip-Error-Toast, le toast n'est pas affiché (le composant gère le message).
 */
export const errorToastInterceptor: HttpInterceptorFn = (req, next) => {
  const toast = inject(ToastService);
  const skipToast = req.headers.has(SKIP_ERROR_TOAST_HEADER);
  let reqToSend: HttpRequest<unknown> = req;
  if (skipToast) {
    reqToSend = req.clone({ headers: req.headers.delete(SKIP_ERROR_TOAST_HEADER) });
  }

  return next(reqToSend).pipe(
    catchError((err: HttpErrorResponse) => {
      if (err?.status !== 401 && !skipToast) {
        const message = extractMessage(err);
        toast.error(message);
      }
      return throwError(() => err);
    })
  );
};
