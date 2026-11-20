import { bootstrapApplication } from "@angular/platform-browser";
import { provideHttpClient, withInterceptors, withXsrfConfiguration } from "@angular/common/http";
import { provideRouter } from "@angular/router";
import { AppComponent } from "./app/app.component";
import { sessaoExpirada } from "./app/services/sessao-expirada.interceptor";
import { routes } from "./app/app.routes";
import { registerLocaleData } from "@angular/common";
import pt from "@angular/common/locales/pt";
registerLocaleData(pt);
bootstrapApplication(AppComponent, {
  providers: [
    provideHttpClient(
      withInterceptors([sessaoExpirada]),
      withXsrfConfiguration({
        cookieName: "XSRF-TOKEN",
        headerName: "X-XSRF-TOKEN",
      }),
    ),
    provideRouter(routes),
  ],
}).catch(console.error);
