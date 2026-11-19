import {Component, inject, signal} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {ApiService} from '../services/api.service';
import {Categoria,Tipo} from '../models/financeiro';
@Component({standalone:true,imports:[FormsModule],template:`
 <div class="page-heading"><div><span class="eyebrow">A ORGANIZAÇÃO É SUA</span><h1>Categorias</h1><p class="muted">Dê nome ao que faz parte da sua vida.</p></div></div>
 @if(erro()) {<p class="notice error" role="alert">{{erro()}}</p>}@if(mensagem()) {<p class="notice" role="status">{{mensagem()}}</p>}
 <section class="panel"><h2>{{editando?'Editar categoria':'Sua próxima categoria'}}</h2><form class="inline-form" (ngSubmit)="salvar()" #form="ngForm"><label>Nome<input name="nome" [(ngModel)]="nome" maxlength="80" required placeholder="Ex.: Meu café favorito"></label><label>Tipo<select name="tipo" [(ngModel)]="tipo"><option value="DESPESA">Despesa</option><option value="RECEITA">Receita</option></select></label><label>Cor<input name="cor" type="color" [(ngModel)]="cor" required></label><button class="primary" [disabled]="form.invalid||salvando()">{{salvando()?'Salvando…':editando?'Salvar alteração':'Criar categoria'}}</button>@if(editando) {<button type="button" class="secondary" (click)="limpar()">Cancelar</button>}</form></section>
 <div class="category-grid">@for(c of categorias();track c.id) {<article class="panel category-card" [class.inactive]="!c.ativa"><div class="category-icon" [style.color]="c.cor" aria-hidden="true">◇</div><div><h2>{{c.nome}}</h2><p>{{c.tipo==='RECEITA'?'Receita':'Despesa'}} · {{c.usuarioId?'Personalizada':'Padrão'}} · {{c.ativa?'Ativa':'Inativa'}}</p></div>@if(c.usuarioId) {<div class="row-actions"><button class="text-button" (click)="editar(c)">Editar</button><button class="text-button" (click)="alternar(c)">{{c.ativa?'Desativar':'Ativar'}}</button></div>}</article>}</div><p class="field-help">Categorias inativas são preservadas no histórico e deixam de ser oferecidas em novos registros e à IA.</p>
 `})
export class CategoriesComponent {
 private api=inject(ApiService);categorias=signal<Categoria[]>([]);erro=signal('');mensagem=signal('');salvando=signal(false);editando?:Categoria;
 nome='';tipo:Tipo='DESPESA';cor='#197456';
 constructor(){void this.carregar();}async carregar(){try{this.categorias.set(await this.api.get<Categoria[]>('/categorias'));}catch(e){this.erro.set(this.api.mensagem(e));}}
 editar(c:Categoria){this.editando=c;this.nome=c.nome;this.tipo=c.tipo;this.cor=c.cor;window.scrollTo({top:0,behavior:'smooth'});}
 limpar(){this.editando=undefined;this.nome='';this.tipo='DESPESA';this.cor='#197456';}
 async salvar(){this.salvando.set(true);this.erro.set('');try{const d={nome:this.nome,tipo:this.tipo,cor:this.cor,ativa:this.editando?.ativa??true};if(this.editando)await this.api.put('/categorias/'+this.editando.id,d);else await this.api.post('/categorias',d);this.limpar();this.mensagem.set('Categoria salva.');await this.carregar();}catch(e){this.erro.set(this.api.mensagem(e));}finally{this.salvando.set(false);}}
 async alternar(c:Categoria){try{await this.api.put('/categorias/'+c.id,{nome:c.nome,tipo:c.tipo,cor:c.cor,ativa:!c.ativa});await this.carregar();}catch(e){this.erro.set(this.api.mensagem(e));}}
}
