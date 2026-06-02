import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class PaymentService {
  constructor(private client: HttpClient) {}

  // ====================================================================
  // PAGOS DEL PROPIETARIO (Suscripción al registrarse)
  // ====================================================================

  prepay(planId: number, token: string): Observable<any> {
    return this.client.get(
      `http://127.0.0.1:8080/payments/prepay?planId=${planId}&token=${token}`,
      { observe: 'response', responseType: 'text', withCredentials: true },
    );
  }

  confirm(response: any, transactionId: string, token: string): Observable<any> {
    response.transactionId = transactionId;
    response.token = token;
    return this.client.post<any>('http://127.0.0.1:8080/payments/confirm', response, {
      observe: 'response',
      withCredentials: true,
    });
  }

  // ====================================================================
  // PAGOS DE CLIENTES (Añadir canción VIP)
  // ====================================================================

  // 1. Pide el secreto a Java para poder abrir el formulario lila de Stripe
  iniciarPagoCancion(barId: string, track: any): Observable<any> {
    // Transformamos el objeto gigante de Spotify a lo que espera tu base de datos
    const peticion = {
      spotifyUri: track.uri,
      trackName: track.name,
      artist: track.artists[0].name,
      albumImageUrl: track.album.images[2].url,
    };

    return this.client.post<any>(
      `http://127.0.0.1:8080/payments/bar/${barId}/intent-cancion`,
      peticion,
    );
  }

  // 2. Avisa a Java de que el pago en Stripe salió bien para activar la canción
  confirmarCancionPagada(barId: string, trackId: string): Observable<any> {
    return this.client.get(
      `http://127.0.0.1:8080/payments/bar/${barId}/success-cancion?track_id=${trackId}`,
    );
  }

  //  NUEVO MÉTODO PARA PREGUNTAR EL PRECIO
  obtenerPrecioBar(barId: string) {
    return this.client.get<{ precio: number }>(
      `http://127.0.0.1:8080/payments/bar/${barId}/precio`,
    );
  }
}
