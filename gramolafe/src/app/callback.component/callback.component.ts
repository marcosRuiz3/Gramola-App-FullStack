import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { SpotifyService } from '../spotify.service'; // Ajusta tu ruta

@Component({
  selector: 'app-callback',
  standalone: true,
  template: `<div style="text-align: center; margin-top: 50px;">
    <h2>Conectando con Spotify... por favor espera 🎵.</h2>
  </div>`,
})
export class CallbackComponent implements OnInit {
  // 1. EL ESCUDO: Creamos la bandera para evitar peticiones duplicadas
  peticionEnviada: boolean = false;

  constructor(
    private route: ActivatedRoute,
    private spotifyService: SpotifyService,
    private router: Router,
  ) {}

  ngOnInit() {
    // 1. Leemos la URL para buscar el "?code=..."
    this.route.queryParams.subscribe((params) => {
      const code = params['code'];

      //  2. LA ADUANA: Solo pasamos si hay código Y si no hemos enviado ya la petición
      if (code && !this.peticionEnviada) {
        //  3. CERRAMOS LA PUERTA: Bloqueamos peticiones fantasma de inmediato
        this.peticionEnviada = true;

        //  4. Esperamos 200ms
        // Esto soluciona el error 401 porque da tiempo al Interceptor
        // a detectar la cookie que acabas de guardar en el localStorage.
        setTimeout(() => {
          this.spotifyService.vincularCuentaBackend(code).subscribe({
            next: () => {
              console.log('¡Tokens guardados en la BD!');
              localStorage.setItem('isSpotifyLinked', 'true');

              // ¡Éxito! Devolvemos al dueño del bar a su Dashboard
              this.router.navigate(['/dashboard']);
            },
            error: (err) => {
              console.error('Error al vincular con Java:', err);
              // Si el error es un 401, el log del interceptor nos dirá si leyó null
              alert('Hubo un error al vincular la cuenta. Revisa la consola.');

              // Abrimos la puerta por si hay que reintentar
              this.peticionEnviada = false;
              this.router.navigate(['/dashboard']);
            },
          });
        }, 200); // 200 milisegundos de margen de seguridad
      } else if (!code) {
        // Si el usuario le dio a "Cancelar" en Spotify, no habrá código
        this.router.navigate(['/dashboard']);
      }
    });
  }
}
