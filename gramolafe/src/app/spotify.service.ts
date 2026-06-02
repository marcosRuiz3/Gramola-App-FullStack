import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { delay, Subject } from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class SpotifyService {
  // Configuraciones fijas de Spotify
  private authorizeUrl = 'https://accounts.spotify.com/authorize';
  private redirectUrl = 'http://127.0.0.1:4200/callback';
  private scopes = [
    'user-modify-playback-state',
    'user-read-playback-state',
    'streaming', // EL PERMISO VITAL PARA ESCUCHAR LA CANCIÓN ENTERA
    'user-read-email', // Spotify lo exige para el SDK Web
    'user-read-private', // Spotify lo exige para el SDK Web
  ];

  constructor(private http: HttpClient) {}

  public eventoCambioCancion = new Subject<void>();

  getToken() {
    // Generamos el código de seguridad
    let state = this.generateString(16);

    let clientId = sessionStorage.getItem('clientId');

    // Construimos los parámetros con backticks (`)
    let params = 'response_type=code';
    params += `&client_id=${clientId}`;
    params += `&scope=${encodeURIComponent(this.scopes.join(' '))}`;
    params += `&redirect_uri=${this.redirectUrl}`;
    params += `&state=${state}`;

    // Guardamos el state para comprobarlo a la vuelta
    sessionStorage.setItem('oauth_state', state);

    // Redirigimos al usuario a la página oficial de Spotify
    let url = this.authorizeUrl + '?' + params;
    window.location.href = url;
  }

  // Método auxiliar para generar la cadena aleatoria del 'state'
  private generateString(length: number): string {
    const characters = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789';
    let result = '';
    for (let i = 0; i < length; i++) {
      result += characters.charAt(Math.floor(Math.random() * characters.length));
    }
    return result;
  }

  vincularCuentaBackend(codigoSpotify: string) {
    // Añadimos un pequeño delay de 100ms para asegurar que el interceptor lea la cookie recién guardada
    return this.http
      .post('http://127.0.0.1:8080/api/spotify/vincular', { code: codigoSpotify })
      .pipe(delay(100));
  }

  // En spotify.service.ts
  buscarCanciones(query: string) {
    // Llamamos a nuestro Java, no a Spotify directamente
    return this.http.get<any>(`http://127.0.0.1:8080/api/spotify/search?q=${query}`);
  }

  addSongToQueue(track: any) {
    // En Spotify, el ID de reproducción se llama 'uri' (ej: spotify:track:4iV5W...)
    const payload = {
      spotifyUri: track.uri,
      trackName: track.name,
      artist: track.artists[0].name, // Pillamos el artista principal
      albumImageUrl: track.album.images[2].url, // La imagen más pequeña
    };

    return this.http.post('http://127.0.0.1:8080/api/spotify/add-to-queue', payload);
  }

  getQueue() {
    return this.http.get<any[]>('http://127.0.0.1:8080/api/spotify/queue');
  }

  getAccessToken() {
    return this.http.get<{ token: string }>('http://127.0.0.1:8080/api/spotify/token');
  }

  cambiarEstadoCancion(uri: string, estado: string) {
    const body = {
      spotifyUri: uri,
      nuevoEstado: estado,
    };
    return this.http.put('http://127.0.0.1:8080/api/spotify/change-state', body);
  }

  // Un método para que alguien pulse el botón del chivato
  notificarCambio() {
    this.eventoCambioCancion.next();
  }

  saltarSiguiente() {
    return this.http.post('http://127.0.0.1:8080/api/spotify/next', {});
  }

  volverAnterior() {
    return this.http.post('http://127.0.0.1:8080/api/spotify/previous', {});
  }

  getHistorial() {
    return this.http.get<any[]>('http://127.0.0.1:8080/api/spotify/history');
  }
}
