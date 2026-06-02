import { Component, OnInit } from '@angular/core';
import { SpotifyService } from '../spotify.service'; // Asegúrate de que la ruta sea la correcta

@Component({
  selector: 'app-historial',
  templateUrl: './historial.component.html',
  styleUrls: ['./historial.component.css'],
})
export class HistorialComponent implements OnInit {
  historialDeHoy: any[] = [];

  constructor(private spotifyService: SpotifyService) {}

  ngOnInit(): void {
    this.cargarHistorial();
  }

  cargarHistorial() {
    this.spotifyService.getHistorial().subscribe({
      next: (tracks) => {
        this.historialDeHoy = tracks;
        console.log('📜 Historial cargado de forma independiente:', tracks);
      },
      error: (err) => console.error('❌ Error al cargar el historial:', err),
    });
  }
}
