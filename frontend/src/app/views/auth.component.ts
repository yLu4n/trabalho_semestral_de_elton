import {Component, inject, signal} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {ActivatedRoute, Router} from '@angular/router';
import {AuthService} from '../services/auth.service';
import {ApiService} from '../services/api.service';
@Component({standalone:true,imports:[FormsModule],template:`
 <div class="auth-layout"><section class="auth-story"><div class="brand"><span class="brand-mark">f<span>·</span></span>fin<span class="brand-ai">ai</span></div><div><span class="eyebrow light">DINHEIRO ORGANIZADO. MENTE TRANQUILA.</span><h1>Suas finanças <br>começam com <br><em>uma frase.</em></h1><p>Conte o que gastou. Confira os detalhes.<br>O FinAI cuida da organização com você.</p><div class="phrase-preview"><span>✧</span> “Almocei 38 reais na faculdade ontem”<div><span>Alimentação</span><strong>R$ 38,00</strong><small>Você sempre confirma antes de salvar.</small></div></div></div><small>Controle pessoal, com a sua decisão em primeiro lugar.</small></section>
 <section class="auth-form"><div class="auth-card"><span class="eyebrow">BEM-VINDO AO SEU ESPAÇO</span><h2>{{verificando?'Confirme seu e-mail':cadastro?'Crie sua conta':'Bom ter você por aqui.'}}</h2><p class="muted">{{cadastro?'Uma visão mais clara da sua vida financeira começa aqui.':'Entre para acompanhar suas finanças com tranquilidade.'}}</p>
 @if(mensagem()) {<div class="notice" role="status">{{mensagem()}}</div>}
 @if(erro()) {<div class="notice error" role="alert">{{erro()}}</div>}
 @if(verificando) {<button class="primary wide" (click)="verificar()" [disabled]="ocupado()">{{ocupado()?'Verificando…':'Confirmar meu e-mail'}}</button><button class="text-button wide" (click)="voltar()">Ir para o login</button>}
 @else {<form (ngSubmit)="enviar()" #form="ngForm">
 @if(cadastro) {<label>Seu nome<input name="nome" [(ngModel)]="nome" required maxlength="100" autocomplete="name" placeholder="Como podemos chamar você?"></label>}
 <label>E-mail<input name="email" [(ngModel)]="email" required type="email" maxlength="254" autocomplete="email" placeholder="voce@exemplo.com"></label>
 <label>Senha<input name="senha" [(ngModel)]="senha" required type="password" [minlength]="cadastro?12:1" maxlength="72" [autocomplete]="cadastro?'new-password':'current-password'" placeholder="Sua senha"></label>
 @if(cadastro) {<p class="field-help">Use pelo menos 12 caracteres, com maiúscula, minúscula, número e símbolo.</p><label>Data de nascimento<input name="nascimento" type="date" [(ngModel)]="nascimento" required></label><label class="checkbox"><input name="consentimento" type="checkbox" [(ngModel)]="consentimento" required>Tenho 18 anos ou mais e li a informação de privacidade abaixo.</label><p class="field-help">Usamos seus dados para manter sua conta e seus registros. Na IA, enviamos o texto informado e categorias; diagnósticos usam somente totais anônimos. Você pode exportar os dados e solicitar exclusão.</p>}
 <button class="primary wide" [disabled]="ocupado()||form.invalid">{{ocupado()?'Aguarde…':cadastro?'Criar minha conta':'Entrar na minha conta'}}</button>
 </form><p class="auth-toggle">{{cadastro?'Já tem uma conta?':'Primeira vez por aqui?'}} <button class="text-button" (click)="alternar()">{{cadastro?'Entrar':'Criar conta'}}</button></p>}
 <div class="auth-security">♧ Seus dados financeiros ficam no seu espaço.</div></div></section></div>
 `})
export class AuthComponent {
 private auth=inject(AuthService); private api=inject(ApiService); private router=inject(Router); private route=inject(ActivatedRoute);
 cadastro=false; verificando=this.router.url.startsWith('/verificar'); nome=''; email=''; senha=''; nascimento=''; consentimento=false;
 ocupado=signal(false); erro=signal(''); mensagem=signal('');
 alternar() {this.cadastro=!this.cadastro; this.erro.set(''); this.mensagem.set('');}
 async enviar() {this.ocupado.set(true); this.erro.set(''); try {
   if(this.cadastro) {await this.auth.csrf(); const r=await this.api.post<{mensagem:string}>('/auth/cadastro',{nome:this.nome,email:this.email,senha:this.senha,nascimento:this.nascimento,aceitaPrivacidade:this.consentimento}); this.mensagem.set(r.mensagem); this.cadastro=false; this.senha='';}
   else {await this.auth.entrar(this.email,this.senha); await this.router.navigate([this.auth.perfil()?.papel==='TITULAR'?'/':'/operacao']);}
 } catch(e) {this.erro.set(this.api.mensagem(e));} finally {this.ocupado.set(false);}}
 async verificar() {this.ocupado.set(true); try {await this.auth.csrf(); const r=await this.api.post<{mensagem:string}>('/auth/verificar',{token:this.route.snapshot.queryParamMap.get('token')||''}); this.mensagem.set(r.mensagem);} catch(e){this.erro.set(this.api.mensagem(e));} finally{this.ocupado.set(false);}}
 voltar() {this.verificando=false; void this.router.navigate(['/entrar']);}
}
