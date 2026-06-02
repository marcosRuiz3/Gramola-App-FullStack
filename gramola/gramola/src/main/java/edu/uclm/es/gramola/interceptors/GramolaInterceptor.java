package edu.uclm.es.gramola.interceptors;


import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import edu.uclm.es.gramola.Dao.UserDAO;
import edu.uclm.es.gramola.model.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// @Component es OBLIGATORIO para que Spring sepa que este portero existe y lo construya
@Component
public class GramolaInterceptor implements HandlerInterceptor {

    // Inyectamos el DAO o el Service para poder preguntar a la base de datos
    private final UserDAO userDao; 

    public GramolaInterceptor(UserDAO userDao) {
        this.userDao = userDao;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        
        // 1. EL SALVACAÍDAS CORS: Angular siempre hace una petición OPTIONS antes de la real.
        // El portero SIEMPRE debe dejar pasar los OPTIONS sin pedir llave.
        if (request.getMethod().equalsIgnoreCase("OPTIONS")) {
            return true;
        }

        // 2. Le pedimos al usuario que nos enseñe la llave (la cabecera)
        String cookieCliente = request.getHeader("Gramola-Cookie");

        // 3. Comprobamos si tiene la llave
        if (cookieCliente != null && !cookieCliente.isEmpty()) {
            
            // 4. Vamos a la base de datos a ver si esa llave es auténtica
            // (Asegúrate de tener este método en tu UserDao)
            User user = userDao.findByGramolaCookie(cookieCliente); 

            if (user != null) {
                // ¡La llave es correcta y el bar existe! Le abrimos la puerta.
                return true; 
            }
        }

        // 5. Si no tiene llave, o se la ha inventado, o la hemos borrado: ¡PORRAZO!
        // Le devolvemos un Error 401 (Unauthorized) y no le dejamos pasar al Controller.
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        return false;
    }
}