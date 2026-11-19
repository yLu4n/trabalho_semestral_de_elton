import { inject, Injectable } from "@angular/core";
import { HttpClient, HttpParams } from "@angular/common/http";
import { firstValueFrom } from "rxjs";
@Injectable({ providedIn: "root" })
export class ApiService {
  private http = inject(HttpClient);
  get<T>(
    path: string,
    query: Record<string, string | number | undefined> = {},
  ) {
    let params = new HttpParams();
    for (const [key, val] of Object.entries(query))
      if (val !== undefined && val !== "")
        params = params.set(key, String(val));
    return firstValueFrom(this.http.get<T>("/api" + path, { params }));
  }
  post<T>(path: string, body: unknown = {}) {
    return firstValueFrom(this.http.post<T>("/api" + path, body));
  }
  put<T>(path: string, body: unknown) {
    return firstValueFrom(this.http.put<T>("/api" + path, body));
  }
  delete(path: string) {
    return firstValueFrom(this.http.delete<void>("/api" + path));
  }
  exportar() {
    return firstValueFrom(
      this.http.get("/api/exportacao", { responseType: "blob" }),
    );
  }
  mensagem(error: unknown): string {
    return (
      (error as { error?: { mensagem?: string } })?.error?.mensagem ||
      "Não foi possível conectar ao servidor. Tente novamente."
    );
  }
}
