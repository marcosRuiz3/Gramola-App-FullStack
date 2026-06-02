import { Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms'; // 🔴 IMPORTANTE PARA EL [(ngModel)]
import { RouterLink, Router } from '@angular/router';
import { SpotifyService } from '../spotify.service'; // Asegúrate de que la ruta es correcta
import { UserService } from '../user.service';
import { MusicComponent } from '../music/music.component'; // Importamos el componente de música para mostrarlo cuando Spotify esté vinculado
import { Subscription, interval } from 'rxjs';
import { HistorialComponent } from '../historial.component/historial.component';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink, FormsModule, MusicComponent, HistorialComponent],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.css'], // Supongo que aquí tienes tus estilos glass-panel
})
export class DashboardComponent implements OnInit, OnDestroy {
  // Inyectamos el servicio del profesor
  constructor(
    private spotifyService: SpotifyService,
    private userService: UserService,
    private router: Router,
  ) {}

  isSpotifyLinked: boolean = false;
  nombreBar: string = ''; // Obtenemos el nombre del bar desde el servicio
  miBarId: string = ''; // Guardamos el ID del bar para usarlo en pagos de canciones
  private chivatoSub!: Subscription;
  private autoRefreshSub!: Subscription;
  // 1. Empezamos viendo la parte principal del Dashboard
  vistaActual: 'principal' | 'historial' = 'principal';
  ocultarReproductor: boolean = false; // Variable para controlar la visibilidad del reproductor

  // Variables para el buscador
  searchQuery: string = '';
  resultadosBusqueda: any[] = [];
  buscando: boolean = false;
  colaDeReproduccion: any[] = [];
  spotifyAccessToken: string = ''; // Aquí guardaremos el token de Spotify para pasarlo al MusicComponent
  uriPrimeraCancion: string | undefined; // La URI de la primera canción de la cola para reproducirla al cargar el Dashboard

  ngOnInit(): void {
    // 1. Inicializamos datos de la sesión
    this.isSpotifyLinked = localStorage.getItem('isSpotifyLinked') === 'true';
    this.nombreBar = sessionStorage.getItem('barName') || 'Mi Bar';
    this.miBarId = sessionStorage.getItem('barId') || '';

    // 2. Cargamos la cola por primera vez al entrar
    this.cargarColaDeReproduccion();

    // 3. EL CHIVATO (Sincronización por Eventos locales de tu reproductor)
    this.chivatoSub = this.spotifyService.eventoCambioCancion.subscribe(() => {
      console.log('🔄 Dashboard detectó cambio de música. Recargando cola...');

      // Magia de Arquitecto: Solo gastamos recursos si estamos viendo la cola
      if (this.vistaActual === 'principal') {
        this.cargarColaDeReproduccion();
      }
    });

    //  4. SHORT POLLING (Sincronización con los móviles de los clientes)
    // Le preguntamos a Java cada 5000ms (5 segundos) si hay canciones VIP nuevas
    this.autoRefreshSub = interval(500).subscribe(() => {
      if (this.vistaActual === 'principal' && this.isSpotifyLinked) {
        this.cargarColaDeReproduccion();
      }
    });

    // 5. Conexión con Spotify (Recuperar Token)
    if (this.isSpotifyLinked) {
      this.spotifyService.getAccessToken().subscribe({
        next: (respuesta) => {
          this.spotifyAccessToken = respuesta.token;
        },
        error: (err) => {
          console.error('No se pudo recuperar el token de Spotify', err);
        },
      });
    }
  }

  // 2. El método que cambia la vista (y mantiene la música a salvo)
  cambiarVista(nuevaVista: 'principal' | 'historial') {
    this.vistaActual = nuevaVista;

    // Si volvemos a la pantalla principal, forzamos una recarga de la lista
    if (nuevaVista === 'principal') {
      this.ocultarReproductor = false;
      this.cargarColaDeReproduccion();
    }
    if (nuevaVista === 'historial') {
      this.ocultarReproductor = true; // Ocultamos el reproductor al ir al historial
    }
  }

  ngOnDestroy(): void {
    // Siempre hay que apagar los micrófonos y los relojes al salir
    if (this.chivatoSub) this.chivatoSub.unsubscribe();

    //  Apagamos el temporizador de 5 segundos
    if (this.autoRefreshSub) this.autoRefreshSub.unsubscribe();
  }

  // Esta función es llamada por el botón verde del HTML
  vincularSpotify() {
    this.spotifyService.getToken();
  }

  logout() {
    this.userService.logout().subscribe({
      next: () => {
        // 1. Limpiamos TODA la memoria del navegador
        sessionStorage.clear();
        localStorage.clear();

        // 2. Cerramos la puerta y vamos al Login
        this.router.navigate(['/login']);
      },
      error: (err) => {
        console.error('Error al cerrar sesión:', err);
        // Opcional: Incluso si falla el backend, igual queremos echarlo al login
        localStorage.clear();
        sessionStorage.clear();
        this.router.navigate(['/login']);
      },
    });
  }

  // El método mágico que actualiza la pantalla
  cargarColaDeReproduccion() {
    this.spotifyService.getQueue().subscribe({
      next: (tracks) => {
        console.log('Respuesta de Java para la cola:', tracks);

        // 1. PARA LA PANTALLA: Guardamos TODAS las canciones (PLAYING, PAUSED, PENDING)
        // Así tu HTML mostrará la lista entera y su estado real.
        this.colaDeReproduccion = tracks;

        // 2. Buscamos si hay alguna canción activa (PLAYING o PAUSED)
        const cancionActiva = tracks.find(
          (c: any) => c.status === 'PLAYING' || c.status === 'PAUSED',
        );

        // 3. Buscamos la primera que esté esperando (PENDING)
        const primeraPendiente = tracks.find((c: any) => c.status === 'PENDING');

        // 4. Lógica de asignación para el Reproductor (MusicComponent)
        if (cancionActiva) {
          // A) Si hay una canción sonando (o en pausa), el reproductor debe anclarse a esa
          this.uriPrimeraCancion = cancionActiva.spotifyUri;
        } else if (primeraPendiente) {
          // B) Si no hay nada sonando, cogemos la primera de la cola de espera
          this.uriPrimeraCancion = primeraPendiente.spotifyUri;
        } else {
          // C) Si no hay canciones en la BD, vaciamos el reproductor
          this.uriPrimeraCancion = undefined;
        }
      },
      error: (err) => {
        console.error('Error al cargar la cola', err);
      },
    });
  }

  // La función que se ejecuta al darle al botón de buscar
  buscarEnSpotify() {
    if (!this.searchQuery.trim()) return; // Si está vacío, no buscamos

    this.buscando = true;
    this.spotifyService.buscarCanciones(this.searchQuery).subscribe({
      next: (respuesta) => {
        // Spotify devuelve la lista dentro de tracks.items
        this.resultadosBusqueda = respuesta.tracks.items;
        this.buscando = false;
        console.log('Resultados de Spotify:', this.resultadosBusqueda);
      },
      error: (err) => {
        console.error('Error al buscar:', err);
        this.buscando = false;
      },
    });
  }

  agregarCancionACola(track: any) {
    this.spotifyService.addSongToQueue(track).subscribe({
      next: () => {
        // 1. Avisamos al usuario
        alert(`¡${track.name} de ${track.artists[0].name} añadida a la cola!`);

        // 2. Recargamos la lista para que aparezca en pantalla
        this.cargarColaDeReproduccion();
      },
      error: (err) => {
        console.error('Error al añadir a la cola:', err);
        alert('No se pudo añadir la canción a la cola. Inténtalo de nuevo.');
      },
    });
  }
}
