import { Component, OnInit, OnDestroy } from '@angular/core'; //  Añadido OnDestroy
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { SpotifyService } from '../spotify.service';
import { PaymentService } from '../payment.service';
import { interval, Subscription } from 'rxjs'; //  Importante para el tiempo real

@Component({
  selector: 'app-vista-cliente',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './vista-cliente.component.html',
  styleUrl: './vista-cliente.component.css',
})
export class VistaClienteComponent implements OnInit, OnDestroy {
  colaDeReproduccion: any[] = [];
  uriPrimeraCancion: string | undefined;

  searchQuery: string = '';
  resultadosBusqueda: any[] = [];
  buscando: boolean = false;

  barId: string = '';
  precioCancion: number = 1.0;

  //  Variable para controlar el refresco automático
  private autoRefreshSub!: Subscription;

  constructor(
    private spotifyService: SpotifyService,
    private paymentService: PaymentService,
    private route: ActivatedRoute,
    private router: Router,
  ) {}

  ngOnInit() {
    // 1. Sacamos el ID de la URL
    this.barId = this.route.snapshot.paramMap.get('id') || '';

    if (this.barId) {
      // 2. Carga inicial
      this.cargarColaDeReproduccion();

      this.paymentService.obtenerPrecioBar(this.barId).subscribe({
        next: (respuesta) => {
          this.precioCancion = respuesta.precio;
        },
        error: (err) => console.error('No se pudo cargar el precio del bar', err),
      });

      //  3. Sincronización automática: Cada 5 segundos preguntamos si ha cambiado la música
      this.autoRefreshSub = interval(500).subscribe(() => {
        this.cargarColaDeReproduccion();
      });
    } else {
      console.error('Error: No se encontró el ID del bar en la URL');
    }
  }

  //  Limpieza: Cuando el cliente cierra la pestaña, apagamos el reloj
  ngOnDestroy() {
    if (this.autoRefreshSub) {
      this.autoRefreshSub.unsubscribe();
    }
  }

  cargarColaDeReproduccion() {
    // Nota: Si tu servicio permite filtrar por barId, pásalo aquí
    this.spotifyService.getQueue().subscribe({
      next: (tracks) => {
        this.colaDeReproduccion = tracks;

        // Buscamos estados para el reproductor visual
        const cancionActiva = tracks.find(
          (c: any) => c.status === 'PLAYING' || c.status === 'PAUSED',
        );
        const primeraPendiente = tracks.find((c: any) => c.status === 'PENDING');

        if (cancionActiva) {
          this.uriPrimeraCancion = cancionActiva.spotifyUri;
        } else if (primeraPendiente) {
          this.uriPrimeraCancion = primeraPendiente.spotifyUri;
        } else {
          this.uriPrimeraCancion = undefined;
        }
      },
      error: (err) => console.error('Error al cargar la cola', err),
    });
  }

  buscarEnSpotify() {
    if (!this.searchQuery.trim()) return;
    this.buscando = true;
    this.spotifyService.buscarCanciones(this.searchQuery).subscribe({
      next: (respuesta) => {
        this.resultadosBusqueda = respuesta.tracks.items;
        this.buscando = false;
      },
      error: (err) => {
        console.error('Error al buscar:', err);
        this.buscando = false;
      },
    });
  }

  iniciarPagoConStripe(track: any) {
    if (!this.barId) {
      alert('Error: No sabemos en qué bar estás.');
      return;
    }

    this.buscando = true;

    this.paymentService.iniciarPagoCancion(this.barId, track).subscribe({
      next: (respuesta) => {
        // Navegamos a la pasarela de pago lila
        this.router.navigate(['/payment'], {
          queryParams: {
            tipo: 'cancion',
            secret: respuesta.clientSecret,
            trackId: respuesta.trackId,
            barId: this.barId,
          },
        });
      },
      error: (err) => {
        console.error('Error al iniciar el pago', err);
        alert('Hubo un problema al contactar con el servidor de pago. Inténtalo de nuevo.');
        this.buscando = false;
      },
    });
  }
}
