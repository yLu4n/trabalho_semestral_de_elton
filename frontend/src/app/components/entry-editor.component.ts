import {AfterViewInit, Component, ElementRef, EventEmitter, inject, Input, Output, signal, ViewChild} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {ApiService} from '../services/api.service';
import {Categoria, Entrada, hoje, Lancamento, Sugestao, Tipo} from '../models/financeiro';
@Component({selector:'entry-editor',standalone:true,imports:[FormsModule],template:`
 <dialog #dialog (cancel)="fechar(false)"><div class="dialog-head"><div><span class="eyebrow">{{sugestao?'REVISÃO DO LANÇAMENTO':'SEU REGISTRO'}}</span><h2>{{lancamento?'Editar lançamento':sugestao?'Confira os detalhes':'Novo lançamento'}}</h2></div><button class="icon-button" (click)="fechar(false)" aria-label="Fechar formulário">×</button></div>
 @if(sugestao?.aviso) {<p class="notice">{{sugestao?.aviso}}</p>}
 @if(sugestao?.conferirValor) {<p class="notice warning" role="alert">Confira o valor com atenção. A IA não teve confiança suficiente.</p>}
 @if(erro()) {<p class="notice error" role="alert">{{erro()}}</p>}
 <form (ngSubmit)="salvar()" #form="ngForm"><div class="form-grid">
 <label [class.pending]="pendente('tipo')">Tipo @if(pendente('tipo')) {<small>Pendente de revisão</small>}<select name="tipo" [(ngModel)]="dados.tipo" required (ngModelChange)="dados.categoriaId=''"><option value="">Selecione</option><option value="DESPESA">Despesa</option><option value="RECEITA">Receita</option></select></label>
 <label [class.pending]="pendente('valor')">Valor (R$) @if(pendente('valor')) {<small>Pendente de revisão</small>}<input name="valor" [(ngModel)]="dados.valor" required inputmode="decimal" pattern="[0-9]+([.,][0-9]{1,2})?" placeholder="0,00"></label>
 <label [class.pending]="pendente('data')">Data @if(pendente('data')) {<small>Pendente de revisão</small>}<input name="data" [(ngModel)]="dados.data" type="date" required></label>
 <label [class.pending]="pendente('categoriaId')">Categoria @if(pendente('categoriaId')) {<small>Pendente de revisão</small>}<select name="categoriaId" [(ngModel)]="dados.categoriaId" required><option value="">Selecione</option>@for(c of categorias;track c.id) {@if(c.tipo===dados.tipo && (c.ativa || c.id===lancamento?.categoriaId)) {<option [value]="c.id">{{c.nome}}{{c.ativa?'':' (inativa)'}}</option>}}</select></label>
 <label class="span-2" [class.pending]="pendente('descricao')">Descrição<input name="descricao" [(ngModel)]="dados.descricao" required maxlength="280" placeholder="O que foi esse lançamento?"></label>
 <label [class.pending]="pendente('estabelecimento')">Estabelecimento <small>Opcional</small><input name="estabelecimento" [(ngModel)]="dados.estabelecimento" maxlength="120" placeholder="Onde aconteceu?"></label>
 <label [class.pending]="pendente('formaPagamento')">Forma de pagamento <small>Opcional</small><input name="formaPagamento" [(ngModel)]="dados.formaPagamento" maxlength="60" placeholder="Pix, dinheiro, cartão…"></label>
 </div><div class="dialog-actions"><button type="button" class="secondary" (click)="fechar(false)" [disabled]="salvando()">Descartar</button><button class="primary" [disabled]="form.invalid||salvando()">{{salvando()?'Salvando…':sugestao&&!sugestao.manual?'Confirmar e salvar':'Salvar lançamento'}}</button></div><p class="field-help">O registro só será salvo quando você confirmar.</p></form></dialog>
 `})
export class EntryEditorComponent implements AfterViewInit {
 @Input() categorias:Categoria[]=[]; @Input() sugestao?:Sugestao; @Input() lancamento?:Lancamento;
 @Output() closed=new EventEmitter<boolean>(); @ViewChild('dialog') dialog!:ElementRef<HTMLDialogElement>;
 private api=inject(ApiService); salvando=signal(false); erro=signal('');
 dados:Entrada={tipo:'DESPESA',valor:'',data:hoje(),categoriaId:'',descricao:'',estabelecimento:'',formaPagamento:''};
 ngAfterViewInit() {if(this.lancamento) this.dados={...this.lancamento}; else if(this.sugestao) {
   const campos=this.sugestao.campos; this.dados={tipo:(campos.tipo||'') as Tipo,valor:campos.valor||'',data:campos.data||'',categoriaId:campos.categoriaId||'',descricao:campos.descricao||'',estabelecimento:campos.estabelecimento||'',formaPagamento:campos.formaPagamento||''};
 } this.dialog.nativeElement.showModal();}
 pendente(campo:string) {return this.sugestao?.pendentes.includes(campo)||false;}
 async salvar() {this.salvando.set(true); this.erro.set(''); try {const entrada={...this.dados,valor:this.dados.valor.replace(',','.')};
   if(this.lancamento) await this.api.put('/lancamentos/'+this.lancamento.id,entrada);
   else if(this.sugestao?.id && !this.sugestao.manual) await this.api.post('/ia/sugestoes/'+this.sugestao.id+'/confirmar',{confirmado:true,lancamento:entrada});
   else await this.api.post('/lancamentos',entrada);
   this.fechar(true);
 } catch(e){this.erro.set(this.api.mensagem(e));} finally{this.salvando.set(false);}}
 fechar(salvo:boolean) {if(this.salvando()&&!salvo) return; this.dialog.nativeElement.close(); if(!salvo&&this.sugestao?.id) void this.api.delete('/ia/sugestoes/'+this.sugestao.id).catch(()=>{}); this.closed.emit(salvo);}
}
