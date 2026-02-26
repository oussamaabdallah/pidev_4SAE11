import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, map, of, catchError, forkJoin } from 'rxjs';
import { environment } from '../../../environments/environment';

/** Réponse LibreTranslate : translatedText peut être une chaîne ou un tableau. */
export interface LibreTranslateResponse {
  translatedText: string | string[];
}

/**
 * Service de traduction côté client via LibreTranslate (API gratuite, sans clé).
 * Utilisé pour les formulaires add/edit offer et la page show-offer.
 * Déclenchement au clic sur "Translate" (pas en ngDoCheck) pour éviter les appels excessifs.
 */
@Injectable({
  providedIn: 'root',
})
export class TranslateService {
  /** URL par défaut : instance gratuite sans clé. Peut être surchargée via environment. */
  private apiUrl =
    (environment as { libretranslateUrl?: string }).libretranslateUrl ??
    'https://libretranslate.de/translate';

  constructor(private http: HttpClient) {}

  /**
   * Traduit un seul texte.
   * @param text Texte à traduire
   * @param source Code langue source (ex: 'en') ou 'auto'
   * @param target Code langue cible (ex: 'fr', 'ar')
   */
  translate(
    text: string,
    source: string,
    target: string
  ): Observable<LibreTranslateResponse> {
    const headers = new HttpHeaders({ 'Content-Type': 'application/json' });
    const body = {
      q: text,
      source,
      target,
      format: 'text',
    };
    return this.http.post<LibreTranslateResponse>(this.apiUrl, body, {
      headers,
    });
  }

  /**
   * Traduit plusieurs textes vers la langue cible.
   * Tente d'abord un seul appel avec q en tableau ; en cas d'échec, un appel par texte (forkJoin).
   */
  translateBatch(
    texts: string[],
    target: string,
    source: string = 'auto'
  ): Observable<string[]> {
    if (!texts?.length) return of([]);
    const trimmed = texts.map((t) => (t ?? '').trim() || ' ');
    const headers = new HttpHeaders({ 'Content-Type': 'application/json' });

    const oneRequest$ = this.http
      .post<LibreTranslateResponse>(this.apiUrl, {
        q: trimmed.length === 1 ? trimmed[0] : trimmed,
        source,
        target,
        format: 'text',
      }, { headers })
      .pipe(
        map((res) => {
          const out = res?.translatedText;
          if (Array.isArray(out)) return out as string[];
          if (typeof out === 'string') return trimmed.length === 1 ? [out] : [out];
          return trimmed;
        })
      );

    return oneRequest$.pipe(
      catchError(() => {
        const singles = trimmed.map((text) =>
          this.translate(text, source, target).pipe(
            map((r) => (typeof r.translatedText === 'string' ? r.translatedText : (r.translatedText as string[])?.[0] ?? text))
          )
        );
        return forkJoin(singles);
      })
    );
  }
}
