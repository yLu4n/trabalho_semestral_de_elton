import {Component, inject, signal} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {ApiService} from '../services/api.service';
import {Categoria, hoje, Meta} from '../models/financeiro';
@Component({standalone:true,imports:[CommonModule,FormsModule],template:`
 <div class="page-heading"><div><span class="eyebrow">ESPAÇO PARA SEUS PLANOS</span><h1>Metas de orçamento</h1><p class="muted">Planeje com liberdade. Acompanhe com clareza.</p></div><label class="month-picker">Mês<input type="month" name="mes" [(ngModel)]="mes" (change)="carregar()"></label></div>
 @if(erro()) {<p class="notice error" role="alert">{{erro()}}</p>}@if(mensagem()) {<p class="notice" role="status">{{mensagem()}}</p>}
 <section class="panel"><h2>Defina um limite para o mês</h2><p class="muted">Uma meta por categoria. Salvar novamente atualiza o limite.</p><form class="inline-form" (ngSubmit)="salvar()" #form="ngForm"><label>Categoria<select name="categoria" [(ngModel)]="categoriaId" required><option value="">Selecione uma despesa</option>@for(c of categorias();track c.id) {@if(c.ativa&&c.tipo==='DESPESA') {<option [value]="c.id">{{c.nome}}</option>}}</select></label><label>Limite mensal (R$)<input name="limite" [(ngModel)]="limite" inputmode="decimal" pattern="[0-9]+([.,][0-9]{1,2})?" placeholder="500,00" required></label><button class="primary" [disabled]="form.invalid||salvando()">{{salvando()?'Salvando…':'Salvar meta'}}</button></form></section>
 <div class="budget-grid">@for(m of metas();track m.id) {<article class="panel"><div class="section-heading"><h2>{{m.categoria}}</h2><button class="text-button" (click)="categoriaId=m.categoriaId;limite=m.limite">Editar limite</button></div><strong class="budget-amount">{{m.consumido|currency:'BRL':'symbol':'1.2-2':'pt-BR'}}</strong><p class="muted">de {{m.limite|currency:'BRL':'symbol':'1.2-2':'pt-BR'}} planejados</p><progress [value]="m.percentual" max="100" [attr.aria-label]="m.categoria+': '+m.percentual+'%'"></progress><div class="list-row"><strong [class.warning-text]="m.estado==='ATENCAO'" [class.danger-text]="m.estado==='CRITICO'">{{m.percentual}}% · {{estado(m.estado)}}</strong><button class="text-button danger-text" (click)="excluir(m)">Remover</button></div></article>} @empty {<div class="panel empty-state"><span>◎</span><h2>Um plano começa com uma escolha.</h2><p>Selecione uma categoria e defina sua primeira meta do mês.</p></div>}</div><p class="field-help">Atenção a partir de 80%. Estado crítico acima de 100%. Ultrapassar o limite não impede novos registros.</p>
 `})
export class BudgetsComponent {
 private api=inject(ApiService); mes=hoje().slice(0,7);categoriaId='';limite='';categorias=signal<Categoria[]>([]);metas=signal<Meta[]>([]);erro=signal('');mensagem=signal('');salvando=signal(false);
 constructor(){void this.carregar();}
 async carregar(){try{const [c,m]=await Promise.all([this.api.get<Categoria[]>('/categorias'),this.api.get<Meta[]>('/metas',{mes:this.mes})]);this.categorias.set(c);this.metas.set(m);}catch(e){this.erro.set(this.api.mensagem(e));}}
 async salvar(){this.salvando.set(true);this.erro.set('');try{await this.api.post('/metas',{categoriaId:this.categoriaId,mes:this.mes,limite:this.limite.replace(',','.')});this.mensagem.set('Meta salva. Você pode ajustar seu plano a qualquer momento.');await this.carregar();}catch(e){this.erro.set(this.api.mensagem(e));}finally{this.salvando.set(false);}}
 async excluir(m:Meta){if(!confirm('Remover a meta de '+m.categoria+'?'))return;try{await this.api.delete('/metas/'+m.id);await this.carregar();}catch(e){this.erro.set(this.api.mensagem(e));}}
 estado(e:string){return e==='CRITICO'?'Acima do limite':e==='ATENCAO'?'Atenção':'Dentro do planejado';}
}
