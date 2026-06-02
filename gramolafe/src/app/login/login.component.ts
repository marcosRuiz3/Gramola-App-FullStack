import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { UserService } from '../user.service'; // Ajusta la ruta si es necesario

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [RouterLink, CommonModule, ReactiveFormsModule],
  templateUrl: './login.component.html',
  styleUrl: './login.component.css',
})
export class LoginComponent implements OnInit {
  loginForm: FormGroup;
  showSuccessMessage: boolean = false; // Para el mensaje de "Vengo del pago"
  loginSuccess: boolean = false; // Para el mensaje de "Login correcto"
  submitted: boolean = false; // Para mostrar errores en el HTML solo al pulsar el botón
  formInvalid: boolean = false; // Para controlar la animación de vibración del botón

  constructor(
    private formBuilder: FormBuilder,
    private route: ActivatedRoute,
    private userService: UserService,
    private router: Router,
  ) {
    this.loginForm = this.formBuilder.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(8)]],
    });
  }

  ngOnInit(): void {
    localStorage.clear();
    sessionStorage.clear();
    console.log('🧼 Navegador limpio para nuevo registro');
    // Detectamos si venimos de un registro/pago con éxito vía URL
    this.route.queryParams.subscribe((params) => {
      if (params['registered'] === 'true') {
        this.showSuccessMessage = true;
        // El mensaje desaparece a los 5 segundos
        setTimeout(() => {
          this.showSuccessMessage = false;
        }, 5000);
      }
    });
  }

  onSubmit() {
    // 1. El usuario acaba de pulsar el botón. Encendemos el interruptor de errores.
    this.submitted = true;

    // 2. Comprobamos si el formulario tiene fallos
    if (this.loginForm.invalid) {
      this.formInvalid = true; // Activamos la vibración del botón
      setTimeout(() => (this.formInvalid = false), 400); // La apagamos para que pueda repetirse
      return; // Detenemos la función aquí para no llamar al backend
    }

    // 3. Si todo está correcto, extraemos los datos
    const email = this.loginForm.value.email;
    const password = this.loginForm.value.password;

    sessionStorage.clear();
    localStorage.clear();

    // 4. Llamamos al método login del servicio
    this.userService.login(email, password).subscribe({
      next: (response: any) => {
        localStorage.clear();
        sessionStorage.clear();
        sessionStorage.setItem('clientId', response.clientId.toString());
        sessionStorage.setItem('barName', response.barName);
        sessionStorage.setItem('barId', response.barId.toString());
        localStorage.setItem('gramola_cookie', response.gramola_cookie);
        localStorage.setItem('isSpotifyLinked', response.isSpotifyLinked.toString());
        setTimeout(() => {
          this.loginSuccess = true;
          this.showSuccessMessage = false;
          this.router.navigate(['/dashboard']);
        }, 50);

        console.log('Sesión iniciada:', response);
      },
      error: (err: any) => {
        console.error('Error en login:', err);
        alert('Email o contraseña incorrectos. Verifica que hayas pagado la cuota.');
        this.loginSuccess = false;
      },
    });
  }
}
