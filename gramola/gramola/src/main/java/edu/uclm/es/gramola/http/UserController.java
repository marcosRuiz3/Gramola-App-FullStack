package edu.uclm.es.gramola.http;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import edu.uclm.es.gramola.model.User;
import edu.uclm.es.gramola.services.UserService;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("users")
@CrossOrigin(
    origins = { "http://localhost:4200", "http://127.0.0.1:4200" }, 
    allowCredentials = "true", 
    allowedHeaders = "*"
)
public class UserController {

    @Autowired
    private UserService service;

    @PostMapping("/register")
    public void register(@RequestBody Map<String, String> body) {
        // Extracción de todos los campos del cuerpo de la petición [cite: 211-216]
        String bar = body.get("bar");
        String email = body.get("email");
        String pwd1 = body.get("pwd1");
        String pwd2 = body.get("pwd2");
        String clientId = body.get("clientId");
        String clientSecret = body.get("clientSecret");

        // Validaciones de los datos recibidos
        if (bar == null || email == null || pwd1 == null || pwd2 == null || clientId == null || clientSecret == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Faltan campos obligatorios");
        }
        if (!pwd1.equals(pwd2)) {
            throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE, "Las contraseñas no coinciden");
        }
        if (pwd1.length() < 8) {
            throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE, "La contraseña debe tener al menos 8 caracteres");
        }
        if (!email.contains("@") || !email.contains(".")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Formato de email inválido");
        }
        
        this.service.register(bar, email, pwd1, clientId, clientSecret);
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> body) {
        
        // 1. Cazamos los datos exactos que manda Angular
        String email = body.get("email");
        String pwd = body.get("pwd"); 
        
        // 2. El Service hace la comprobación y crea la sesión (gramola_cookie)
        User user = this.service.login(email, pwd);
        
        // 3. Preparamos la "caja" JSON perfecta
        Map<String, Object> response = new HashMap<>();
        response.put("clientId", user.getClientId());
        response.put("gramola_cookie", user.getGramolaCookie());
        response.put("barId", user.getId());

        // Comprobamos si el usuario ya tiene un token guardado
        boolean estaVinculado = user.getSpotifyAccessToken() != null;
        response.put("isSpotifyLinked", estaVinculado);
        response.put("barName", user.getBar());
        
        // 4. Se la enviamos a Angular (Código 200 OK)
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public void logout(@RequestHeader("Gramola-Cookie") String gramolaCookie) { // Leemos del Header
        if (gramolaCookie == null || gramolaCookie.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Falta la cookie de sesión");
        }
        this.service.logout(gramolaCookie);
    }
    
    /**
     * Confirma el token de un usuario y lo redirige a la pasarela de pago.
     * Este método es invocado cuando el usuario hace clic en el enlace de su correo. [cite: 241]
     */
    @GetMapping("/confirmToken/{email}")
    public void confirmToken(@PathVariable String email, @RequestParam String token, HttpServletResponse response) throws IOException {
        // 1. Llama al servicio para validar el token
        this.service.confirmToken(email, token);
        
        // 2. Si la validación es exitosa, redirige al front-end para el pago 
        response.sendRedirect("http://127.0.0.1:4200/select-plan?token=" + token);
    }

    @DeleteMapping("/delete")
    public void delete(@RequestParam String email) {
        this.service.delete(email);
    }

    @PostMapping("/request-reset")
    public void requestReset(@RequestBody Map<String, String> data) {
        String email = data.get("email");
        this.service.requestPasswordReset(email);
    }

    @PostMapping("/confirm-reset")
    public void confirmReset(@RequestBody Map<String, String> data) {
        String token = data.get("token");
        String newPassword = data.get("password");
        this.service.confirmPasswordReset(token, newPassword);
    }
}