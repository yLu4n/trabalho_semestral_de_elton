import { Component, inject, signal } from "@angular/core";
import { FormsModule } from "@angular/forms";
import { ActivatedRoute, Router, RouterLink } from "@angular/router";
import { AuthService } from "../services/auth.service";
import { ApiService } from "../services/api.service";
@Component({
  standalone: true,
  imports: [FormsModule, RouterLink],
  template: `
    <div class="auth-layout">
      <section class="auth-story">
        <div class="brand">
          <span class="brand-mark">f<span>·</span></span
          >fin<span class="brand-ai">ai</span>
        </div>
        <div>
          <span class="eyebrow light"
            >DINHEIRO ORGANIZADO. MENTE TRANQUILA.</span
          >
          <h1>Suas finanças <br />começam com <br /><em>uma frase.</em></h1>
          <p>
            Conte o que gastou. Confira os detalhes.<br />O FinAI cuida da
            organização com você.
          </p>
          <div class="phrase-preview">
            <span>✧</span> “Almocei 38 reais na faculdade ontem”
            <div>
              <span>Alimentação</span><strong>R$ 38,00</strong
              ><small>Você sempre confirma antes de salvar.</small>
            </div>
          </div>
        </div>
        <small>Controle pessoal, com a sua decisão em primeiro lugar.</small>
      </section>
      <section class="auth-form">
        <div class="auth-card">
          <span class="eyebrow">BEM-VINDO AO SEU ESPAÇO</span>
          <h2>
            {{
              verificando
                ? "Confirme seu e-mail"
                : recuperando
                  ? "Recupere sua senha"
                  : redefinindo
                    ? "Defina uma nova senha"
                    : cadastro
                      ? "Crie sua conta"
                      : "Bom ter você por aqui."
            }}
          </h2>
          <p class="muted">
            {{
              cadastro
                ? "Uma visão mais clara da sua vida financeira começa aqui."
                : "Entre para acompanhar suas finanças com tranquilidade."
            }}
          </p>
          @if (mensagem()) {
            <div class="notice" role="status">{{ mensagem() }}</div>
          }
          @if (erro()) {
            <div class="notice error" role="alert">{{ erro() }}</div>
          }
          @if (verificando) {
            <button
              class="primary wide"
              (click)="verificar()"
              [disabled]="ocupado()"
            >
              {{ ocupado() ? "Verificando…" : "Confirmar meu e-mail" }}</button
            ><button class="text-button wide" (click)="voltar()">
              Ir para o login
            </button>
          } @else if (recuperando || redefinindo) {
            <form (ngSubmit)="salvarRecuperacao()" #recuperacaoForm="ngForm">
              @if (recuperando) {
                <label
                  >E-mail da conta<input
                    name="email"
                    [(ngModel)]="email"
                    required
                    type="email"
                    maxlength="254"
                    autocomplete="email"
                /></label>
              } @else {
                <label
                  >Nova senha<input
                    name="senha"
                    [(ngModel)]="senha"
                    required
                    type="password"
                    minlength="8"
                    maxlength="72"
                    [pattern]="politicaSenha"
                    autocomplete="new-password"
                /></label>
                <p class="field-help">
                  Use pelo menos oito caracteres, incluindo uma letra e um
                  número.
                </p>
                @if (senhaMuitoLonga()) {
                  <p class="field-help" role="alert">A senha ultrapassa o limite de 72 bytes.</p>
                }
              }
              <button
                class="primary wide"
                [disabled]="ocupado() || recuperacaoForm.invalid || (redefinindo && senhaMuitoLonga())"
              >
                {{
                  recuperando
                    ? "Enviar vínculo de recuperação"
                    : "Redefinir senha"
                }}
              </button>
            </form>
            <a class="text-link" routerLink="/entrar">Voltar para o login</a>
          } @else {
            <form (ngSubmit)="enviar()" #form="ngForm">
              @if (cadastro) {
                <label
                  >Seu nome<input
                    name="nome"
                    [(ngModel)]="nome"
                    required
                    maxlength="100"
                    autocomplete="name"
                    placeholder="Como podemos chamar você?"
                /></label>
              }
              <label
                >E-mail<input
                  name="email"
                  [(ngModel)]="email"
                  required
                  type="email"
                  maxlength="254"
                  autocomplete="email"
                  placeholder="voce@exemplo.com"
              /></label>
              <label
                >Senha<input
                  name="senha"
                  [(ngModel)]="senha"
                  required
                  type="password"
                  [minlength]="cadastro ? 8 : 1"
                  [pattern]="cadastro ? politicaSenha : '.*'"
                  maxlength="72"
                  [autocomplete]="
                    cadastro ? 'new-password' : 'current-password'
                  "
                  placeholder="Sua senha"
              /></label>
              @if (cadastro) {
                <p class="field-help">
                  Use pelo menos oito caracteres, incluindo uma letra e um
                  número.
                </p>
                @if (senhaMuitoLonga()) {
                  <p class="field-help" role="alert">A senha ultrapassa o limite de 72 bytes.</p>
                }
                <label
                  >Data de nascimento<input
                    name="nascimento"
                    type="date"
                    [(ngModel)]="nascimento"
                    required /></label
                ><label class="checkbox"
                  ><input
                    name="consentimento"
                    type="checkbox"
                    [(ngModel)]="consentimento"
                    required
                  />Tenho 18 anos ou mais e aceito os termos de uso e a política
                  de privacidade abaixo.</label
                >
                <p class="field-help">
                  Termos de uso. O FinAI organiza registros financeiros
                  pessoais. O titular deve usar a própria conta e conferir seus
                  lançamentos.
                </p>
                <p class="field-help">
                  Política de privacidade. Usamos seus dados para manter sua
                  conta e seus registros. Na IA, enviamos o texto informado e
                  categorias; diagnósticos usam somente totais anônimos. Você
                  pode exportar os dados e solicitar exclusão.
                </p>
              }
              <button
                class="primary wide"
                [disabled]="ocupado() || form.invalid || (cadastro && senhaMuitoLonga())"
              >
                {{
                  ocupado()
                    ? "Aguarde…"
                    : cadastro
                      ? "Criar minha conta"
                      : "Entrar na minha conta"
                }}
              </button>
            </form>
            @if (!cadastro) {
              <p>
                <a class="text-link" routerLink="/recuperar"
                  >Esqueci minha senha</a
                >
              </p>
            }
            <p class="auth-toggle">
              {{ cadastro ? "Já tem uma conta?" : "Primeira vez por aqui?" }}
              <button class="text-button" (click)="alternar()">
                {{ cadastro ? "Entrar" : "Criar conta" }}
              </button>
            </p>
          }
          <div class="auth-security">
            ♧ Seus dados financeiros ficam no seu espaço.
          </div>
        </div>
      </section>
    </div>
  `,
})
export class AuthComponent {
  politicaSenha = /^(?=.{8})(?=.*\p{L})(?=.*\p{Nd})/su;
  senhaMuitoLonga() {
    return new TextEncoder().encode(this.senha).length > 72;
  }
  private auth = inject(AuthService);
  private api = inject(ApiService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  cadastro = false;
  verificando = this.route.snapshot.routeConfig?.path === "verificar";
  get recuperando() {
    return this.route.snapshot.routeConfig?.path === "recuperar";
  }
  get redefinindo() {
    return this.route.snapshot.routeConfig?.path === "redefinir";
  }
  nome = "";
  email = "";
  senha = "";
  nascimento = "";
  consentimento = false;
  ocupado = signal(false);
  erro = signal("");
  mensagem = signal(
    this.route.snapshot.queryParamMap.has("senhaRedefinida")
      ? "Senha redefinida. Entre com a nova senha."
      : "",
  );
  alternar() {
    this.cadastro = !this.cadastro;
    this.erro.set("");
    this.mensagem.set("");
  }
  async enviar() {
    this.ocupado.set(true);
    this.erro.set("");
    try {
      if (this.cadastro) {
        await this.auth.csrf();
        const r = await this.api.post<{ mensagem: string }>("/auth/cadastro", {
          nome: this.nome,
          email: this.email,
          senha: this.senha,
          nascimento: this.nascimento,
          aceitaPrivacidade: this.consentimento,
        });
        this.mensagem.set(r.mensagem);
        this.cadastro = false;
        this.senha = "";
      } else {
        await this.auth.entrar(this.email, this.senha);
        await this.router.navigate([
          this.auth.perfil()?.papel === "TITULAR" ? "/" : "/operacao",
        ]);
      }
    } catch (e) {
      this.erro.set(this.api.mensagem(e));
    } finally {
      this.ocupado.set(false);
    }
  }
  async salvarRecuperacao() {
    this.ocupado.set(true);
    this.erro.set("");
    this.mensagem.set("");
    try {
      await this.auth.csrf();
      if (this.recuperando) {
        const r = await this.api.post<{ mensagem: string }>("/auth/recuperar", {
          email: this.email,
        });
        this.mensagem.set(r.mensagem);
      } else {
        await this.api.post("/auth/redefinir", {
          token: this.route.snapshot.queryParamMap.get("token") || "",
          senha: this.senha,
        });
        this.senha = "";
        await this.router.navigate(["/entrar"], {
          queryParams: { senhaRedefinida: "sim" },
        });
      }
    } catch (e) {
      this.erro.set(this.api.mensagem(e));
    } finally {
      this.ocupado.set(false);
    }
  }
  async verificar() {
    this.ocupado.set(true);
    try {
      await this.auth.csrf();
      const r = await this.api.post<{ mensagem: string }>("/auth/verificar", {
        token: this.route.snapshot.queryParamMap.get("token") || "",
      });
      this.mensagem.set(r.mensagem);
    } catch (e) {
      this.erro.set(this.api.mensagem(e));
    } finally {
      this.ocupado.set(false);
    }
  }
  voltar() {
    this.verificando = false;
    void this.router.navigate(["/entrar"]);
  }
}
