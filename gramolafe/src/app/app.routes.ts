import { Routes } from '@angular/router';
import { RegisterComponent } from './register/register.component';
import { LoginComponent } from './login/login.component';
import { PaymentComponent } from './payments/payment.component';
import { PlanSelectionComponent } from './plan-selection/plan-selection.component';
import { RecoverypwdComponent } from './recoverypwd.component/recoverypwd.component';
import { DashboardComponent } from './dashboard.component/dashboard.component';
import { CallbackComponent } from './callback.component/callback.component';
import { HistorialComponent } from './historial.component/historial.component';
import { VistaClienteComponent } from './vista-cliente.component/vista-cliente.component';

export const routes: Routes = [
  { path: '', redirectTo: 'login', pathMatch: 'full' },
  { path: 'login', component: LoginComponent },
  { path: 'register', component: RegisterComponent },
  { path: 'payment', component: PaymentComponent },
  { path: 'select-plan', component: PlanSelectionComponent },
  { path: 'recoverypwd', component: RecoverypwdComponent },

  { path: 'bar/:id', component: VistaClienteComponent },

  {
    path: 'dashboard',
    component: DashboardComponent,
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      { path: 'historial', component: HistorialComponent },
    ],
  },
  { path: 'callback', component: CallbackComponent },

  { path: '**', redirectTo: 'login' },
];
