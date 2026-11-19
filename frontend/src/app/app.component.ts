import {Component, inject, signal} from '@angular/core';
import {Router, RouterLink, RouterLinkActive, RouterOutlet} from '@angular/router';
import {AuthService} from './services/auth.service';
@Component({selector:'app-root',standalone:true,imports:[RouterOutlet,RouterLink,RouterLinkActive],template:`
 <a class="skip-link" href="#conteudo">Pular para o conteúdo</a>
 @if(auth.perfil(); as perfil) {
 <div class="shell"><aside class="sidebar" [class.expanded]="menu()">
  <a routerLink="/" class="brand" aria-label="FinAI, início"><span class="brand-mark">f<span>·</span></span>fin<span class="brand-ai">ai</span></a>
  <div class="workspace-label">SUAS FINANÇAS, COM CLAREZA</div>
  <nav aria-label="Navegação principal">
  @if(perfil.papel==='TITULAR') {
   <a routerLink="/" routerLinkActive="active" [routerLinkActiveOptions]="{exact:true}" (click)="menu.set(false)"><span aria-hidden="true">▦</span>Visão geral</a>
   <a routerLink="/lancamentos" routerLinkActive="active" (click)="menu.set(false)"><span aria-hidden="true">≡</span>Lançamentos</a>
   <a routerLink="/metas" routerLinkActive="active" (click)="menu.set(false)"><span aria-hidden="true">◎</span>Metas de orçamento</a>
   <a routerLink="/categorias" routerLinkActive="active" (click)="menu.set(false)"><span aria-hidden="true">◇</span>Categorias</a>
  } @else {<a routerLink="/operacao" routerLinkActive="active">Operação do sistema</a>}
  </nav>
  <div class="sidebar-bottom"><div class="sidebar-note"><span class="sparkle" aria-hidden="true">✧</span><strong>Menos esforço.<br>Mais controle.</strong><p>Uma frase pode ser o começo de um novo hábito.</p></div>
   @if(perfil.papel==='TITULAR') {<a routerLink="/privacidade" class="settings-link">Privacidade e conta</a>}
   <div class="profile"><div class="avatar">{{perfil.nome.charAt(0).toUpperCase()}}</div><div><strong>{{perfil.nome}}</strong><small>{{perfil.papel==='TITULAR'?'Conta pessoal':perfil.papel}}</small></div><button class="icon-button" (click)="sair()" aria-label="Sair da conta">⏻</button></div>
  </div>
 </aside><div class="main-wrap"><header class="topbar"><button class="mobile-menu icon-button" (click)="menu.set(!menu())" [attr.aria-expanded]="menu()" aria-label="Abrir menu">☰</button><span class="topbar-breadcrumb">Meu espaço <span>/</span> Finanças pessoais</span><span class="private-badge"><span aria-hidden="true">♧</span> Espaço privado</span></header><main id="conteudo" tabindex="-1"><router-outlet/></main><footer>FinAI <span>Feito para simplificar seu dia a dia.</span></footer></div></div>
 } @else {<main id="conteudo" tabindex="-1"><router-outlet/></main>}
 `})
export class AppComponent {
 auth=inject(AuthService); router=inject(Router); menu=signal(false);
 constructor() {void this.auth.carregar();}
 async sair() {try {await this.auth.sair();} finally {await this.router.navigate(['/entrar']);}}
}
