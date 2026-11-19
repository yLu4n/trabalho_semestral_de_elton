import { bootstrapApplication } from "@angular/platform-browser";
import { provideHttpClient, withXsrfConfiguration } from "@angular/common/http";
import { provideRouter } from "@angular/router";
import { AppComponent } from "./app/app.component";
import { routes } from "./app/app.routes";
import { registerLocaleData } from "@angular/common";
import pt from "@angular/common/locales/pt";
registerLocaleData(pt);
bootstrapApplication(AppComponent, {
  providers: [
    provideHttpClient(
      withXsrfConfiguration({
        cookieName: "XSRF-TOKEN",
        headerName: "X-XSRF-TOKEN",
      }),
    ),
    provideRouter(routes),
  ],
}).catch(console.error);
