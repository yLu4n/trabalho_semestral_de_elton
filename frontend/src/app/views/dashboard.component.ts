import { Component, inject, signal } from "@angular/core";
import { CommonModule } from "@angular/common";
import { FormsModule } from "@angular/forms";
import { RouterLink } from "@angular/router";
import { ApiService } from "../services/api.service";
import { AuthService } from "../services/auth.service";
import {
  Categoria,
  Diagnostico,
  hoje,
  limitesMes,
  Meta,
  Pagina,
  Painel,
  Sugestao,
} from "../models/financeiro";
import { EntryEditorComponent } from "../components/entry-editor.component";
@Component({
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, EntryEditorComponent],
  template: `
    <div class="page-heading">
      <div>
        <span class="eyebrow">UM PASSO DE CADA VEZ</span>
        <h1>Olá, {{ nome() }}<span class="hello-dot">.</span></h1>
        <p class="muted">Uma visão clara para cuidar do que importa.</p>
      </div>
      <label class="month-picker"
        ><span>Período</span
        ><input
          type="month"
          name="mes"
          [(ngModel)]="mes"
          (change)="carregar()"
          required
          aria-label="Mês do painel"
      /></label>
    </div>
    @if (erro()) {
      <div class="notice error" role="alert">
        {{ erro() }}
        <button class="text-button" (click)="carregar()">
          Tentar novamente
        </button>
      </div>
    }
    @if (emailPendente()) {
      <p class="notice" role="status">
        Verifique o e-mail pelo vínculo enviado no cadastro. Enquanto a
        verificação estiver pendente, você pode criar até cinco lançamentos.
      </p>
    }
    <section class="quick-entry" aria-labelledby="registro-titulo">
      <div class="quick-heading">
        <span class="ai-symbol" aria-hidden="true">✧</span>
        <div>
          <h2 id="registro-titulo">O que movimentou seu dia?</h2>
          <p>Conte em uma frase. A IA organiza, você confirma.</p>
        </div>
        <span class="chip">COM INTELIGÊNCIA ARTIFICIAL</span>
      </div>
      <form (ngSubmit)="interpretar()">
        <div class="phrase-input">
          <label class="sr-only" for="frase"
            >Descreva uma receita ou despesa</label
          ><input
            id="frase"
            name="frase"
            [(ngModel)]="frase"
            maxlength="280"
            required
            placeholder="Ex.: Almocei 38 reais na faculdade ontem"
            [disabled]="interpretando()"
          /><button
            class="primary"
            [disabled]="interpretando() || !frase.trim()"
          >
            {{ interpretando() ? "Interpretando…" : "Organizar com IA" }}
            <span aria-hidden="true">✧</span>
          </button>
        </div>
      </form>
      <div class="quick-foot">
        <span
          >{{ frase.length }}/280 caracteres · Você tem a palavra final.</span
        ><button class="text-button" (click)="abrirManual()">
          Prefiro registrar manualmente
        </button>
      </div>
      @if (interpretando()) {
        <p role="status" class="progress-message">
          <span class="spinner"></span>Interpretando sua frase. Você poderá
          revisar cada detalhe.
        </p>
      }
    </section>
    @if (carregando()) {
      <p role="status" class="muted">Atualizando sua visão financeira…</p>
    }
    @if (painel(); as p) {
      <section class="stat-grid" aria-label="Resumo do período">
        <article class="stat-card balance">
          <div>
            <span>Saldo do período</span><span aria-hidden="true">◈</span>
          </div>
          <strong>{{
            p.totais.saldo | currency: "BRL" : "symbol" : "1.2-2" : "pt-BR"
          }}</strong
          ><small>Receitas menos despesas</small>
        </article>
        <article class="stat-card">
          <div>
            <span>Receitas</span
            ><span class="stat-icon income" aria-hidden="true">+</span>
          </div>
          <strong>{{
            p.totais.receitas | currency: "BRL" : "symbol" : "1.2-2" : "pt-BR"
          }}</strong
          ><small>Entradas no período selecionado</small>
        </article>
        <article class="stat-card">
          <div>
            <span>Despesas</span
            ><span class="stat-icon expense" aria-hidden="true">−</span>
          </div>
          <strong>{{
            p.totais.despesas | currency: "BRL" : "symbol" : "1.2-2" : "pt-BR"
          }}</strong
          ><small>Saídas no período selecionado</small>
        </article>
      </section>
      <div class="dashboard-grid">
        <section class="panel">
          <div class="section-heading">
            <div>
              <h2>Para onde vai seu dinheiro</h2>
              <p>Despesas por categoria</p>
            </div>
            <span class="mini-icon" aria-hidden="true">◴</span>
          </div>
          @if (p.distribuicao.length) {
            <div class="distribution">
              <div
                class="donut"
                [style.background]="gradiente()"
                role="img"
                aria-label="Distribuição de despesas por categoria; valores na tabela ao lado"
              >
                <div>
                  <span>Total de despesas</span
                  ><strong>{{
                    p.totais.despesas
                      | currency: "BRL" : "symbol" : "1.2-2" : "pt-BR"
                  }}</strong>
                </div>
              </div>
              <table class="distribution-table">
                <caption class="sr-only">
                  Despesas e percentual por categoria
                </caption>
                <thead class="sr-only">
                  <tr>
                    <th>Categoria</th>
                    <th>Valor</th>
                    <th>Percentual</th>
                  </tr>
                </thead>
                <tbody>
                  @for (c of p.distribuicao; track c.categoriaId) {
                    <tr>
                      <th>
                        <span
                          class="color-dot"
                          [style.background]="c.cor"
                        ></span
                        >{{ c.categoria }}
                      </th>
                      <td>
                        {{
                          c.valor
                            | currency: "BRL" : "symbol" : "1.2-2" : "pt-BR"
                        }}
                      </td>
                      <td>{{ c.percentual }}%</td>
                    </tr>
                  }
                </tbody>
              </table>
            </div>
          } @else {
            <div class="empty-state">
              <span aria-hidden="true">◌</span>
              <h3>Cada registro traz mais clareza.</h3>
              <p>Suas despesas aparecerão aqui assim que você começar.</p>
            </div>
          }
        </section>
        <section class="panel budget-panel">
          <div class="section-heading">
            <div>
              <h2>Seu orçamento, no ritmo certo</h2>
              <p>Metas de {{ mes }}</p>
            </div>
            <a routerLink="/metas" class="text-link">Gerenciar</a>
          </div>
          @for (m of metas(); track m.id) {
            <div class="budget-item">
              <div>
                <strong>{{ m.categoria }}</strong
                ><span
                  class="budget-status"
                  [class.warning-text]="m.estado === 'ATENCAO'"
                  [class.danger-text]="m.estado === 'CRITICO'"
                  >{{ estado(m.estado) }}</span
                >
              </div>
              <progress
                [value]="m.percentual"
                max="100"
                [attr.aria-label]="
                  m.categoria + ': ' + m.percentual + '% do orçamento'
                "
              ></progress
              ><small
                >{{
                  m.consumido | currency: "BRL" : "symbol" : "1.2-2" : "pt-BR"
                }}
                <span
                  >de
                  {{
                    m.limite | currency: "BRL" : "symbol" : "1.2-2" : "pt-BR"
                  }}
                  · {{ m.percentual }}%</span
                ></small
              >
            </div>
          } @empty {
            <div class="empty-state compact">
              <span aria-hidden="true">◎</span>
              <h3>Dê um destino ao seu dinheiro.</h3>
              <p>Crie limites por categoria e acompanhe o mês.</p>
              <a class="secondary" routerLink="/metas">Definir primeira meta</a>
            </div>
          }
        </section>
      </div>
      <div class="dashboard-grid lower">
        <section class="panel">
          <div class="section-heading">
            <div>
              <h2>Seu dinheiro ao longo do tempo</h2>
              <p>Receitas e despesas nos últimos seis meses</p>
            </div>
          </div>
          <div
            class="bar-chart"
            role="img"
            aria-label="Comparação mensal de receitas e despesas. Dados na tabela abaixo."
          >
            @for (s of p.serie; track s.mes) {
              <div class="bar-column">
                <div class="bar-pair">
                  <div
                    class="bar income-bar"
                    [style.height.%]="altura(s.receitas)"
                  ></div>
                  <div
                    class="bar expense-bar"
                    [style.height.%]="altura(s.despesas)"
                  ></div>
                </div>
                <span>{{ s.mes.slice(5) }}/{{ s.mes.slice(2, 4) }}</span>
              </div>
            }
          </div>
          <div class="chart-legend">
            <span><i class="income-bar"></i>Receitas</span
            ><span><i class="expense-bar"></i>Despesas</span>
          </div>
          <details class="chart-data">
            <summary>Consultar valores do gráfico</summary>
            <div class="table-scroll">
              <table>
                <caption class="sr-only">
                  Comparação mensal
                </caption>
                <thead>
                  <tr>
                    <th>Mês</th>
                    <th>Receitas</th>
                    <th>Despesas</th>
                  </tr>
                </thead>
                <tbody>
                  @for (s of p.serie; track s.mes) {
                    <tr>
                      <th>{{ s.mes }}</th>
                      <td>
                        {{
                          s.receitas
                            | currency: "BRL" : "symbol" : "1.2-2" : "pt-BR"
                        }}
                      </td>
                      <td>
                        {{
                          s.despesas
                            | currency: "BRL" : "symbol" : "1.2-2" : "pt-BR"
                        }}
                      </td>
                    </tr>
                  }
                </tbody>
              </table>
            </div>
          </details>
        </section>
        <section class="insight-card">
          <span class="eyebrow">UM OLHAR PARA O MÊS</span>
          <h2>Pequenos registros.<br />Grandes percepções.</h2>
          <p>
            Projeção linear de despesas para {{ p.mesProjecao }}, com base nos
            dias transcorridos:
          </p>
          <strong class="projection">{{
            p.projecaoDespesas | currency: "BRL" : "symbol" : "1.2-2" : "pt-BR"
          }}</strong
          ><small
            >Uma estimativa do ritmo atual, sem garantia de fechamento.</small
          ><button
            class="secondary"
            (click)="diagnosticar()"
            [disabled]="gerandoDiagnostico()"
          >
            {{
              gerandoDiagnostico()
                ? "Analisando os totais…"
                : "Gerar diagnóstico mensal"
            }}
            <span aria-hidden="true">✧</span>
          </button>
          <p class="field-help">
            Análise descritiva. Não constitui recomendação profissional.
          </p>
        </section>
      </div>
      @if (diagnostico(); as d) {
        <section class="panel diagnosis" aria-live="polite">
          <span class="eyebrow">DIAGNÓSTICO MENSAL</span>
          <h2>Um pouco mais de contexto</h2>
          <p>{{ d.texto }}</p>
          <p class="notice">{{ d.aviso }}</p>
        </section>
      }
      <section class="panel recent">
        <div class="section-heading">
          <div>
            <h2>Últimos movimentos</h2>
            <p>Seu histórico começa em cada detalhe.</p>
          </div>
          <a routerLink="/lancamentos" class="text-link">Ver todos</a>
        </div>
        <div class="table-scroll">
          <table>
            <thead>
              <tr>
                <th>Descrição</th>
                <th>Categoria</th>
                <th>Data</th>
                <th class="align-right">Valor</th>
              </tr>
            </thead>
            <tbody>
              @for (l of recentes(); track l.id) {
                <tr>
                  <td>
                    <strong>{{ l.descricao }}</strong
                    ><small class="block muted">{{
                      l.formaPagamento || "Não informado"
                    }}</small>
                  </td>
                  <td>
                    <span class="category-pill">{{
                      categoria(l.categoriaId)
                    }}</span>
                  </td>
                  <td>{{ l.data | date: "dd/MM/yyyy" : "UTC" }}</td>
                  <td
                    class="align-right"
                    [class.income-text]="l.tipo === 'RECEITA'"
                  >
                    {{ l.tipo === "RECEITA" ? "+" : "−" }}
                    {{
                      l.valor | currency: "BRL" : "symbol" : "1.2-2" : "pt-BR"
                    }}
                  </td>
                </tr>
              } @empty {
                <tr>
                  <td colspan="4" class="empty-table">
                    Nenhum movimento neste mês. Que tal registrar o primeiro?
                  </td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      </section>
    }
    @if (editor()) {
      <entry-editor
        [categorias]="categorias()"
        [sugestao]="sugestao"
        (closed)="fecharEditor($event)"
      />
    }
  `,
})
export class DashboardComponent {
  private api = inject(ApiService);
  private auth = inject(AuthService);
  emailPendente() {
    return this.auth.perfil()?.emailVerificado === false;
  }
  painel = signal<Painel | null>(null);
  categorias = signal<Categoria[]>([]);
  metas = signal<Meta[]>([]);
  recentes = signal<Pagina["itens"]>([]);
  diagnostico = signal<Diagnostico | null>(null);
  carregando = signal(false);
  interpretando = signal(false);
  gerandoDiagnostico = signal(false);
  erro = signal("");
  editor = signal(false);
  sugestao?: Sugestao;
  mes = hoje().slice(0, 7);
  frase = "";
  nome = () => this.auth.perfil()?.nome.split(" ")[0] || "você";
  constructor() {
    void this.carregar();
  }
  async carregar() {
    if (!/^\d{4}-\d{2}$/.test(this.mes)) return;
    this.carregando.set(true);
    this.erro.set("");
    this.diagnostico.set(null);
    try {
      const filtro = limitesMes(this.mes);
      const [p, c, m, l] = await Promise.all([
        this.api.get<Painel>("/painel", filtro),
        this.api.get<Categoria[]>("/categorias"),
        this.api.get<Meta[]>("/metas", { mes: this.mes }),
        this.api.get<Pagina>("/lancamentos", { ...filtro, tamanho: 5 }),
      ]);
      this.painel.set(p);
      this.categorias.set(c);
      this.metas.set(m);
      this.recentes.set(l.itens);
    } catch (e) {
      this.erro.set(this.api.mensagem(e));
    } finally {
      this.carregando.set(false);
    }
  }
  async interpretar() {
    this.interpretando.set(true);
    this.erro.set("");
    try {
      this.sugestao = await this.api.post<Sugestao>("/ia/interpretacoes", {
        texto: this.frase,
      });
      this.editor.set(true);
    } catch (e) {
      this.erro.set(this.api.mensagem(e));
      this.sugestao = {
        id: null,
        campos: { descricao: this.frase },
        confianca: {},
        pendentes: ["tipo", "valor", "data", "categoriaId"],
        conferirValor: false,
        manual: true,
        aviso: "Continue pelo registro manual.",
      };
      this.editor.set(true);
    } finally {
      this.interpretando.set(false);
    }
  }
  abrirManual() {
    this.sugestao = undefined;
    this.editor.set(true);
  }
  fecharEditor(salvo: boolean) {
    this.editor.set(false);
    if (salvo) {
      this.frase = "";
      void this.carregar();
    }
  }
  async diagnosticar() {
    this.gerandoDiagnostico.set(true);
    this.erro.set("");
    try {
      this.diagnostico.set(
        await this.api.post<Diagnostico>("/ia/diagnosticos?mes=" + this.mes),
      );
    } catch (e) {
      this.erro.set(this.api.mensagem(e));
    } finally {
      this.gerandoDiagnostico.set(false);
    }
  }
  categoria(id: string) {
    return this.categorias().find((c) => c.id === id)?.nome || "Categoria";
  }
  estado(estado: string) {
    return estado === "CRITICO"
      ? "Acima do limite"
      : estado === "ATENCAO"
        ? "Atenção ao limite"
        : "Dentro do planejado";
  }
  altura(valor: string) {
    const max = Math.max(
      1,
      ...(this.painel()?.serie || []).flatMap((s) => [
        Number(s.receitas),
        Number(s.despesas),
      ]),
    );
    return (Number(valor) / max) * 100;
  }
  gradiente() {
    let soma = 0;
    return (
      "conic-gradient(" +
      (this.painel()?.distribuicao || [])
        .map((c) => {
          const inicio = soma;
          soma += Number(c.percentual);
          return `${c.cor} ${inicio}% ${soma}%`;
        })
        .join(",") +
      ")"
    );
  }
}
