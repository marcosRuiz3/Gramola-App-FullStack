import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { SubscriptionService } from '../subscription.service'; // Ajusta la ruta

@Component({
  selector: 'app-plan-selection',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './plan-selection.component.html',
  styleUrl: './plan-selection.component.css',
})
export class PlanSelectionComponent implements OnInit {
  plans: any[] = [];
  userToken: string = '';

  constructor(
    private subscriptionService: SubscriptionService,
    private route: ActivatedRoute,
    private router: Router,
  ) {}

  ngOnInit(): void {
    // 1. Recogemos el token de la URL (que nos pasa el backend al hacer login)
    this.route.queryParams.subscribe((params) => {
      this.userToken = params['token'];
    });

    // 2. Pedimos los planes a la base de datos
    this.subscriptionService.getPlans().subscribe({
      next: (data) => {
        this.plans = data;
        console.log('Planes cargados:', this.plans);
      },
      error: (err) => {
        console.error('Error al cargar los planes', err);
      },
    });
  }

  // Método que se ejecuta al hacer clic en "Comprar"
  selectPlan(planId: number) {
    // Viajamos a tu componente de pago original, pasándole el token y el plan
    this.router.navigate(['/payment'], {
      queryParams: {
        token: this.userToken,
        plan: planId,
      },
    });
  }
}
