import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { UserService } from '../user.service';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './register.component.html',
  styleUrl: './register.component.css',
})
export class RegisterComponent implements OnInit {
  bar?: string;
  email?: string;
  clientId?: string;
  clientSecret?: string;
  pwd1?: string;
  pwd2?: string;

  registroOK: boolean = false;
  registroKO: boolean = false;

  constructor(private service: UserService) {}
  ngOnInit(): void {
    //  Vaciamos el bolsillo antes de empezar un registro nuevo
    localStorage.clear();
    sessionStorage.clear();
    console.log('Navegador desinfectado. No hay cookies viejas.');
  }

  registrar() {
    this.registroOK = false;
    this.registroKO = false;

    if (this.pwd1 !== this.pwd2 || (this.pwd1?.length ?? 0) < 8 || (this.pwd2?.length ?? 0) < 8) {
      console.error('Las contraseñas no coinciden o no cumplen con el mínimo de 8 caracteres');
      this.registroKO = true;
      return;
    }

    // Pasamos todos los parámetros requeridos
    this.service
      .register(this.bar!, this.email!, this.pwd1!, this.pwd2!, this.clientId!, this.clientSecret!)
      .subscribe({
        next: (ok) => {
          console.log('Registro exitoso', ok);
          this.registroOK = true;
        },
        error: (error) => {
          console.error('Error en el registro', error);
          this.registroKO = true;
        },
      });
  }
}
