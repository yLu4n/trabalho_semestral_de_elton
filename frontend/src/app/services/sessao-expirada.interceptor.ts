import { HttpErrorResponse, HttpInterceptorFn } from "@angular/common/http";
import { inject, Injector } from "@angular/core";
import { Router } from "@angular/router";
import { tap } from "rxjs";
import { AuthService } from "./auth.service";

export const sessaoExpirada: HttpInterceptorFn = (request, next) => {
  const injector = inject(Injector);
  const router = inject(Router);
  return next(request).pipe(
    tap({
      error: (error: unknown) => {
        if (
          !(error instanceof HttpErrorResponse) ||
          error.status !== 401 ||
          !request.url.startsWith("/api/") ||
          request.url === "/api/auth/login"
        ) return;

        const auth = injector.get(AuthService);
        if (auth.perfil()) {
          auth.limparSessao();
          void router.navigateByUrl("/entrar");
        }
      },
    }),
  );
};
