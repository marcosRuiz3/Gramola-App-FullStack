import { Component, OnInit } from '@angular/core';
import { PaymentService } from '../payment.service';
import { Router } from '@angular/router';

declare let Stripe: any;

@Component({
  selector: 'app-payment',
  standalone: true,
  imports: [],
  templateUrl: './payment.component.html',
  styleUrl: './payment.component.css',
})
export class PaymentComponent implements OnInit {
  stripe = new Stripe(
    'pk_test_51SIV1DAExfNP6UebKlAhyrF5fqHW54lMudBugGqm1QNAoLaFjBbNd5GF4d0DdmrBhQg1fV5n4kGfeVRN8M9HrDE500IcKtA2Eu',
  );
  transactionDetails: any;
  token?: string;
  planId: any;
  pagoExitoso: boolean = false;
  pagando: boolean = false;

  // 🔴 NUEVAS VARIABLES PARA EL PAGO DE CANCIONES
  tipoPago: 'suscripcion' | 'cancion' = 'suscripcion';
  trackIdParaConfirmar?: string;
  barIdDestino?: string;

  constructor(
    private Payments: PaymentService,
    private router: Router,
  ) {}

  ngOnInit(): void {
    const params = this.router.parseUrl(this.router.url).queryParams;

    // 🔀 BIFURCACIÓN 1: ¿De dónde viene el usuario?
    if (params['tipo'] === 'cancion') {
      // CAMINO A: Es un cliente pagando una canción VIP
      this.tipoPago = 'cancion';
      this.trackIdParaConfirmar = params['trackId'];
      this.barIdDestino = params['barId'];

      // Truco maestro: Simulamos el formato que espera tu método showForm()
      this.transactionDetails = {
        data: { client_secret: params['secret'] },
      };

      // Saltamos prepay() y vamos directos a mostrar la tarjeta
      this.showForm();
    } else {
      // CAMINO B: Es un propietario pagando su suscripción (Tu código original)
      this.tipoPago = 'suscripcion';
      this.token = params['token'];
      this.planId = params['plan'];
      this.prepay();
    }
  }

  prepay() {
    if (!this.planId || !this.token) {
      alert('Error: Faltan datos de la suscripción o el token.');
      return;
    }

    this.Payments.prepay(this.planId, this.token).subscribe({
      next: (response: any) => {
        this.transactionDetails = JSON.parse(response.body);
        this.showForm();
      },
      error: (err: any) => {
        alert('Error al inicializar el pago: ' + err);
      },
    });
  }

  showForm() {
    let elements = this.stripe.elements();

    let style = {
      base: {
        color: '#D4B3FF',
        fontFamily: 'system-ui, -apple-system, sans-serif',
        fontSmoothing: 'antialiased',
        fontSize: '16px',
        '::placeholder': {
          color: '#a78bfa',
        },
        iconColor: '#D4B3FF',
      },
      invalid: {
        fontFamily: 'system-ui, -apple-system, sans-serif',
        color: '#fa755a',
        iconColor: '#fa755a',
      },
    };

    let card = elements.create('card', { style: style });
    card.mount('#card-element');

    card.on('change', function (event: any) {
      document.querySelector('button')!.disabled = event.empty;
      document.querySelector('#card-error')!.textContent = event.error ? event.error.message : '';
    });

    let self = this;
    let form = document.getElementById('payment-form');
    form!.addEventListener('submit', function (event) {
      event.preventDefault();
      self.payWithCard(card);
    });
    form!.style.display = 'block';
  }

  payWithCard(card: any) {
    let self = this;
    let secretoStripe = this.transactionDetails.data.client_secret;

    this.stripe
      .confirmCardPayment(secretoStripe, {
        payment_method: {
          card: card,
        },
      })
      .then(function (response: any) {
        if (response.error) {
          alert(response.error.message);
        } else {
          if (response.paymentIntent.status === 'succeeded') {
            self.pagoExitoso = true;

            //  BIFURCACIÓN 2: ¿Qué hacemos tras el éxito?
            if (self.tipoPago === 'cancion') {
              // Éxito de Canción: Avisamos a Java y volvemos al Bar
              self.Payments.confirmarCancionPagada(
                self.barIdDestino!,
                self.trackIdParaConfirmar!,
              ).subscribe({
                next: () => {
                  setTimeout(() => {
                    self.router.navigate(['/bar', self.barIdDestino]);
                  }, 2000);
                },
                error: (error: any) => {
                  alert('Error al confirmar la canción en el servidor: ' + error.message);
                },
              });
            } else {
              // Éxito de Suscripción: Avisamos a Java y vamos al Login
              self.Payments.confirm(response, self.transactionDetails.id, self.token!).subscribe({
                next: (response: any) => {
                  setTimeout(() => {
                    self.router.navigate(['/login'], { queryParams: { registered: 'true' } });
                  }, 2000);
                },
                error: (error: any) => {
                  alert(error);
                },
              });
            }
          }
        }
      });
  }
}
