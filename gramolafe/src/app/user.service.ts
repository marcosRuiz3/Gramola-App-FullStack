import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class UserService {
  // Ojo: he quitado el espacio inicial y he dejado la ruta base
  private apiUrl = 'http://127.0.0.1:8080/users';
  private nombreBarLogueado: string = '';

  constructor(private http: HttpClient) {}

  register(
    bar: string,
    email: string,
    pwd1: string,
    pwd2: string,
    clientId: string,
    clientSecret: string,
  ) {
    let info = {
      bar: bar,
      email: email,
      pwd1: pwd1,
      pwd2: pwd2,
      clientId: clientId,
      clientSecret: clientSecret,
    };
    // Añadimos "/register" a la url base
    return this.http.post<any>(this.apiUrl + '/register', info);
  }

  login(email: string, password: string) {
    let info = {
      email: email,
      pwd: password,
    };
    return this.http.post<any>(this.apiUrl + '/login', info, { withCredentials: true });
  }

  logout() {
    return this.http.post<any>(this.apiUrl + '/logout', {});
  }

  // 2. Método para GUARDAR el nombre (lo usaremos en el Login)
  setNombreBar(nombre: string) {
    this.nombreBarLogueado = nombre;
    // Opcional: Respaldarlo en localStorage solo por si el usuario pulsa F5
    localStorage.setItem('barName', nombre);
  }

  // 3. Método para LEER el nombre (lo usaremos en el Dashboard)
  getNombreBar(): string {
    // Si la variable está vacía (por ejemplo, si recargó la página con F5), miramos el respaldo
    if (!this.nombreBarLogueado) {
      this.nombreBarLogueado = localStorage.getItem('barName') || '';
    }
    return this.nombreBarLogueado;
  }

  requestPasswordReset(email: string): Observable<any> {
    // Mandamos el email como un objeto JSON
    return this.http.post('http://127.0.0.1:8080/users/request-reset', { email });
  }

  confirmPasswordReset(token: string, password: string): Observable<any> {
    // Mandamos el token y la nueva contraseña
    return this.http.post('http://127.0.0.1:8080/users/confirm-reset', { token, password });
  }
}
