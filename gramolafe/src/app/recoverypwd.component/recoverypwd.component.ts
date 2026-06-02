import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { UserService } from '../user.service'; 

@Component({
  selector: 'app-recoverypwd',
  standalone: true,
  imports: [ RouterLink,CommonModule,FormsModule],
  templateUrl: './recoverypwd.component.html',
  styleUrls: ['./recoverypwd.component.css']
})
export class RecoverypwdComponent implements OnInit {
  // Variables para controlar el estado
  token: string | null = null;
  mensajeInfo: string = '';

  // Variables para los inputs del HTML
  email: string = '';
  newPassword: string = '';
  confirmPassword: string = '';

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private userService: UserService
  ) {}

  ngOnInit(): void {
    // Leemos la URL al cargar el componente
    this.route.queryParams.subscribe(params => {
      this.token = params['token'] || null;
    });
  }

  // MÉTODO FASE 1: Pide el correo
  pedirRescate() {
    if (!this.email) {
      alert('Por favor, introduce tu email');
      return;
    }
    
    this.userService.requestPasswordReset(this.email).subscribe({
      next: () => {
        // Mostramos un mensaje y ocultamos el formulario (borrando el email)
        this.mensajeInfo = 'Si el correo existe, te hemos enviado un enlace. Revisa tu bandeja de entrada.';
        this.email = ''; 
      },
      error: (err) => {
        alert('Error al solicitar la recuperación');
      }
    });
  }

  // MÉTODO FASE 2: Cambia la contraseña
  cambiarContrasena() {
    if (this.newPassword !== this.confirmPassword) {
      alert('Las contraseñas no coinciden');
      return;
    }

    if (this.newPassword.length < 4) {
      alert('La contraseña es demasiado corta');
      return;
    }

    this.userService.confirmPasswordReset(this.token!, this.newPassword).subscribe({
      next: () => {
        alert('Contraseña cambiada con éxito. Ya puedes iniciar sesión.');
        this.router.navigate(['/login']); // Lo mandamos directo a loguearse
      },
      error: (err) => {
        alert('Error: El enlace ha caducado o no es válido');
      }
    });
  }
}