import {inject} from '@angular/core';
import {Routes, Router} from '@angular/router';
import {AuthService} from './services/auth.service';
const protegido=async()=> {const auth=inject(AuthService),router=inject(Router);return (await auth.atualizar()) ? true:router.createUrlTree(['/entrar']);};
export const routes:Routes=[
 {path:'entrar',loadComponent:()=>import('./views/auth.component').then(m=>m.AuthComponent)},
 {path:'verificar',loadComponent:()=>import('./views/auth.component').then(m=>m.AuthComponent)},
 {path:'',canActivate:[protegido],loadComponent:()=>import('./views/dashboard.component').then(m=>m.DashboardComponent)},
 {path:'lancamentos',canActivate:[protegido],loadComponent:()=>import('./views/transactions.component').then(m=>m.TransactionsComponent)},
 {path:'metas',canActivate:[protegido],loadComponent:()=>import('./views/budgets.component').then(m=>m.BudgetsComponent)},
 {path:'categorias',canActivate:[protegido],loadComponent:()=>import('./views/categories.component').then(m=>m.CategoriesComponent)},
 {path:'privacidade',canActivate:[protegido],loadComponent:()=>import('./views/privacy.component').then(m=>m.PrivacyComponent)},
 {path:'operacao',canActivate:[protegido],loadComponent:()=>import('./views/operations.component').then(m=>m.OperationsComponent)},
 {path:'**',redirectTo:''}
];
