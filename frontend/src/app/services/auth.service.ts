import {inject, Injectable, signal} from '@angular/core';
import {ApiService} from './api.service';
import {Perfil} from '../models/financeiro';
@Injectable({providedIn:'root'})
export class AuthService {
 private api=inject(ApiService);
 perfil=signal<Perfil|null>(null);
 private carregamento?:Promise<Perfil|null>;
 carregar() {if(this.perfil())return Promise.resolve(this.perfil());return this.carregamento??=this.atualizar();}
 async atualizar() {try {const perfil=await this.api.get<Perfil>('/auth/eu'); this.perfil.set(perfil); return perfil;} catch {this.perfil.set(null); return null;}}
 async csrf() {await this.api.get('/auth/csrf');}
 async entrar(email:string,senha:string) {await this.csrf(); await this.api.post('/auth/login',{email,senha}); await this.csrf(); await this.atualizar();}
 async sair() {await this.api.post('/auth/logout'); this.perfil.set(null); this.carregamento=undefined;}
}
