import { Component, Input, OnInit, OnDestroy } from '@angular/core';
import { SpotifyService } from '../spotify.service';

declare var Spotify: any;

@Component({
  selector: 'app-music',
  templateUrl: './music.component.html',
  styleUrls: ['./music.component.css'],
})
export class MusicComponent implements OnInit, OnDestroy {
  constructor(private spotifyService: SpotifyService) {}

  @Input() token!: string;

  private _autoplayUri?: string;
  private sdkReady = false;

  @Input() set autoplayUri(uri: string | undefined) {
    this._autoplayUri = uri;
    this.intentarAutoplayAutomatico();
  }

  @Input() isVisible: boolean = true;

  player: any;
  deviceId: string = '';

  title = 'Esperando música...';
  artist = 'La Gramola';
  albumArt = '';
  isPaused = true;

  volumen = 0.5;
  progresoMs = 0;
  duracionMs = 1;
  progresoPorcentaje = 0;
  private timerProgreso: any;
  private cancionActualUri: string = '';

  ngOnInit() {
    this.iniciarSpotifySDK();
    this.iniciarTemporizadorVisual();
  }

  iniciarSpotifySDK() {
    const script = document.createElement('script');
    script.src = 'https://sdk.scdn.co/spotify-player.js';
    script.type = 'text/javascript';
    script.async = true;
    script.defer = true;
    document.body.appendChild(script);

    (window as any).onSpotifyWebPlaybackSDKReady = () => {
      this.player = new Spotify.Player({
        name: '🎵 La Gramola Player',
        getOAuthToken: (cb: any) => cb(this.token),
        volume: this.volumen,
      });

      this.player.addListener('ready', ({ device_id }: any) => {
        console.log('✅ Reproductor virtual listo. Device ID:', device_id);
        this.deviceId = device_id;
        this.sdkReady = true;
        this.intentarAutoplayAutomatico();
      });

      this.player.addListener('player_state_changed', (state: any) => {
        if (!state) return;

        const track = state.track_window.current_track;
        const trackUri = track.uri;

        // Guardamos por dónde iba nuestro reloj justo ANTES
        // de que Spotify actualice los números.
        const progresoAnterior = this.progresoMs;

        // 1. ACTUALIZAMOS LA INTERFAZ VISUAL
        this.title = track.name;
        this.artist = track.artists[0].name;

        if (track.album.images && track.album.images.length > 0) {
          this.albumArt = track.album.images[1].url;
        }

        this.progresoMs = state.position;
        this.duracionMs = state.duration;
        this.actualizarPorcentaje();

        //  2. LÓGICA DE ESTADOS BLINDADA
        // Calculamos qué porcentaje de la canción habíamos escuchado.
        const porcentajeCompletado = (progresoAnterior / this.duracionMs) * 100;

        // Si se pausa tras escuchar más del 98%, ¡es el final natural de la canción!
        const esFinDeCancion = state.paused && porcentajeCompletado > 98;

        // Asignamos el estado correcto
        let nuevoEstado;
        if (state.paused) {
          if (esFinDeCancion) {
            nuevoEstado = 'FINISHED';
          } else {
            nuevoEstado = 'PAUSED';
          }
        } else {
          nuevoEstado = 'PLAYING';
        }
        //const nuevoEstado = state.paused ? (esFinDeCancion ? 'FINISHED' : 'PAUSED') : 'PLAYING';

        // 3. SINCRONIZACIÓN CON JAVA
        if (
          this.cancionActualUri !== trackUri ||
          this.isPaused !== state.paused ||
          esFinDeCancion
        ) {
          this.cancionActualUri = trackUri;
          this.isPaused = state.paused;

          console.log(`📡 Sincronizando con Java: ${track.name} -> ${nuevoEstado}`);

          this.spotifyService.cambiarEstadoCancion(trackUri, nuevoEstado).subscribe({
            next: () => {
              if (esFinDeCancion) {
                console.log('🏁 Canción terminada de forma natural y mandada al historial.');
              } else {
                console.log(`✅ BD actualizada correctamente: ${nuevoEstado}`);
              }

              // Avisamos al Dashboard. Al decirle que la canción está FINISHED, el Dashboard
              // la mandará al historial, cogerá la primera en PENDING y nos la pasará
              // automáticamente por el Autoplay.
              this.spotifyService.notificarCambio();
            },
            error: (err) => console.error('❌ Error al contactar con Java:', err),
          });
        }
      });

      this.player.connect();
    };
  }

  //  2. EL NUEVO AUTOPLAY SILENCIOSO
  async intentarAutoplayAutomatico() {
    if (!this.sdkReady || !this._autoplayUri) return;

    console.log('🚀 Intentando Autoplay automático...');

    // Mandamos la orden a Spotify de fondo
    const urlAPI = 'https://api.spotify.com/v1/me/player/play?device_id=' + this.deviceId;
    const body = { uris: [this._autoplayUri] };

    try {
      const response = await fetch(urlAPI, {
        method: 'PUT',
        body: JSON.stringify(body),
        headers: {
          Authorization: `Bearer ${this.token}`,
          'Content-Type': 'application/json',
        },
      });

      if (!response.ok) return;

      console.log('🎶 Orden aceptada. Iniciando intento de arranque...');

      let intentos = 0;
      const radarInterval = setInterval(() => {
        if (!this.player) {
          clearInterval(radarInterval);
          return;
        }

        this.player.getCurrentState().then((state: any) => {
          if (!state) return;

          if (!state.paused) {
            console.log('✅ Autoplay triunfal. Chrome nos ha dado permiso.');
            clearInterval(radarInterval);
            this._autoplayUri = undefined;
            return;
          }

          if (state.paused) {
            // Intentamos arrancar. Si Chrome bloquea esto por falta de interacción, lanzará un error.
            this.player.resume().catch((err: any) => {
              console.warn(
                '❌ Chrome bloqueó el Autoplay (Posible F5). Desplegando cortina de emergencia.',
              );
              clearInterval(radarInterval);
            });
          }
        });

        intentos++;
        if (intentos > 15) {
          clearInterval(radarInterval);
        }
      }, 500);
    } catch (e) {
      console.error('❌ Error de red al contactar con Spotify:', e);
    }
  }

  togglePlay() {
    if (this.player) this.player.togglePlay();
  }

  nextTrack() {
    console.log('⏭ Saltando a la siguiente canción...');

    this.spotifyService.saltarSiguiente().subscribe({
      next: () => {
        console.log('✅ Canción saltada con éxito.');
        // Obligamos a la pantalla a recargar la cola.
        // Como la actual ya está en FINISHED, la pantalla enganchará la siguiente.
        this.spotifyService.notificarCambio();
      },
      error: (err) => console.error('❌ Error al saltar canción:', err),
    });
  }

  previousTrack() {
    console.log('⏮ Volviendo a la canción anterior...');

    this.spotifyService.volverAnterior().subscribe({
      next: () => {
        console.log('✅ Canción anterior resucitada.');
        // Al recargar, la resucitada estará la primera de la lista PENDING y empezará a sonar
        this.spotifyService.notificarCambio();
      },
      error: (err) => console.error('❌ Error al volver atrás:', err),
    });
  }

  cambiarVolumen(event: any) {
    this.volumen = parseFloat(event.target.value);
    if (this.player) {
      this.player.setVolume(this.volumen);
    }
  }

  iniciarTemporizadorVisual() {
    this.timerProgreso = setInterval(() => {
      if (!this.isPaused && this.duracionMs > 1) {
        this.progresoMs += 1000;
        if (this.progresoMs > this.duracionMs) this.progresoMs = this.duracionMs;
        this.actualizarPorcentaje();
      }
    }, 1000);
  }

  actualizarPorcentaje() {
    this.progresoPorcentaje = (this.progresoMs / this.duracionMs) * 100;
  }

  formatearTiempo(ms: number): string {
    const totalSeconds = Math.floor(ms / 1000);
    const minutes = Math.floor(totalSeconds / 60);
    const seconds = totalSeconds % 60;
    return `${minutes}:${seconds < 10 ? '0' : ''}${seconds}`;
  }

  ngOnDestroy() {
    if (this.player) this.player.disconnect();
    if (this.timerProgreso) clearInterval(this.timerProgreso);
  }
}
