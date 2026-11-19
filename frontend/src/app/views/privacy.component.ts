import { Component, inject, signal } from "@angular/core";
import { FormsModule } from "@angular/forms";
import { Router } from "@angular/router";
import { ApiService } from "../services/api.service";
import { AuthService } from "../services/auth.service";
@Component({
  standalone: true,
  imports: [FormsModule],
  template: `
    <div class="page-heading">
      <div>
        <span class="eyebrow">VOCÊ ESTÁ NO CONTROLE</span>
        <h1>Privacidade e conta</h1>
        <p class="muted">Seus dados, suas escolhas.</p>
      </div>
    </div>
    @if (erro()) {
      <p class="notice error" role="alert">{{ erro() }}</p>
    }
    @if (mensagem()) {
      <p class="notice" role="status">{{ mensagem() }}</p>
    }
    <section class="panel readable">
      <h2>Transparência sobre seus dados</h2>
      <p>
        Seus lançamentos pertencem somente a você. Administradores e
        encarregados de dados não têm acesso ao conteúdo financeiro
        identificável pela aplicação.
      </p>
      <p>
        Ao usar o registro por IA, sua frase e as categorias ativas são enviadas
        a um provedor externo. Evite incluir dados bancários, números de cartão
        e dados de outras pessoas. O diagnóstico envia somente totais dos dois
        meses, com categorias substituídas por códigos. A retenção técnica pelo
        provedor depende do contrato e das configurações contratadas.
      </p>
      <p>
        Lançamentos excluídos ficam recuperáveis por 90 dias. Sugestões não
        confirmadas expiram em 30 minutos. A sessão expira após 30 minutos sem
        atividade ou 12 horas de duração.
      </p>
    </section>
    <section class="panel readable">
      <h2>Leve seu histórico com você</h2>
      <p>
        Exporte seus lançamentos ativos em CSV aberto, com codificação UTF-8.
      </p>
      <button class="secondary" (click)="exportar()">Baixar meu extrato</button>
    </section>
    <section class="panel readable">
      <h2>Fale com o encarregado de dados</h2>
      <form class="inline-form" (ngSubmit)="solicitar()">
        <label
          >Solicitação<select name="tipo" [(ngModel)]="tipo">
            <option value="ACESSO">Acesso aos dados</option>
            <option value="CORRECAO">Correção de dados</option>
            <option value="INFORMACOES">Informações sobre o tratamento</option>
          </select></label
        ><button class="secondary">Registrar solicitação</button>
      </form>
      @for (s of solicitacoes(); track s.id) {
        <div class="list-row">
          <span>Protocolo {{ s.id }} · {{ s.tipo }}</span
          ><strong>{{ s.estado }}</strong>
        </div>
      }
    </section>
    <section class="panel danger-panel readable">
      <h2>Solicitar exclusão da conta</h2>
      <p>
        A solicitação encerra suas sessões imediatamente. Os dados pessoais
        serão eliminados em até 15 dias corridos. Registros de auditoria
        anônimos podem ser preservados. Exporte seu extrato antes de continuar.
      </p>
      <form (ngSubmit)="excluir()" #form="ngForm">
        <label
          >Confirme sua senha<input
            name="senha"
            [(ngModel)]="senha"
            type="password"
            required
            autocomplete="current-password" /></label
        ><label class="checkbox"
          ><input
            name="confirmacao"
            type="checkbox"
            [(ngModel)]="confirmacao"
            required
          />Entendo que minha conta e meus dados serão excluídos.</label
        ><button class="danger" [disabled]="form.invalid || ocupado()">
          {{ ocupado() ? "Solicitando…" : "Solicitar exclusão definitiva" }}
        </button>
      </form>
    </section>
  `,
})
export class PrivacyComponent {
  private api = inject(ApiService);
  private auth = inject(AuthService);
  private router = inject(Router);
  erro = signal("");
  mensagem = signal("");
  ocupado = signal(false);
  solicitacoes = signal<{ id: string; tipo: string; estado: string }[]>([]);
  senha = "";
  confirmacao = false;
  tipo = "ACESSO";
  constructor() {
    void this.carregar();
  }
  async carregar() {
    try {
      this.solicitacoes.set(await this.api.get("/privacidade/solicitacoes"));
    } catch (e) {
      this.erro.set(this.api.mensagem(e));
    }
  }
  async exportar() {
    try {
      const blob = await this.api.exportar();
      const url = URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = "finai-extrato.csv";
      a.click();
      setTimeout(() => URL.revokeObjectURL(url), 1000);
    } catch (e) {
      this.erro.set(this.api.mensagem(e));
    }
  }
  async solicitar() {
    try {
      await this.api.post("/privacidade/solicitacoes", { tipo: this.tipo });
      this.mensagem.set(
        "Solicitação registrada. Acompanhe o protocolo abaixo.",
      );
      await this.carregar();
    } catch (e) {
      this.erro.set(this.api.mensagem(e));
    }
  }
  async excluir() {
    if (!this.confirmacao) return;
    this.ocupado.set(true);
    try {
      await this.api.post("/privacidade/exclusao", { senha: this.senha });
      this.auth.perfil.set(null);
      await this.router.navigate(["/entrar"]);
    } catch (e) {
      this.erro.set(this.api.mensagem(e));
    } finally {
      this.ocupado.set(false);
    }
  }
}
