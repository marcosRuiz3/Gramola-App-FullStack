import { HttpInterceptorFn } from '@angular/common/http';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  // 1. Buscamos la cookie en el bolsillo del navegador
  const gramolaCookie = localStorage.getItem('gramola_cookie');
  console.log(
    'Interceptor intentando leer cookie para:',
    req.url,
    'Valor encontrado:',
    gramolaCookie,
  );

  // Si la URL es de login, registro o planes, NO mandamos cookie (queremos ir limpios)
  if (
    req.url.includes('/subscriptions/plans') ||
    req.url.includes('/register') ||
    req.url.includes('/login')
  ) {
    return next(req);
  }

  // 2. Si tenemos la llave, la pegamos en la cabecera de la petición para que Java sepa quién es el cliente
  if (gramolaCookie && req.url.includes('127.0.0.1:8080')) {
    // IMPORTANTE: En Angular las peticiones son inmutables,
    // hay que "clonarlas" para poder modificarlas.
    const peticionClonada = req.clone({
      setHeaders: {
        // Inventamos una cabecera personalizada para que tu Java la reconozca
        'Gramola-Cookie': gramolaCookie,
      },
    });

    // 3. Dejamos que la petición continúe su viaje hacia Java
    return next(peticionClonada);
  }

  // 4. Si NO hay cookie (porque aún no ha hecho login), mandamos la petición tal cual
  return next(req);
};
