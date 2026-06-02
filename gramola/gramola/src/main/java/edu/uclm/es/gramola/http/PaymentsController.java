package edu.uclm.es.gramola.http;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam; // <-- IMPORTANTE: importar esta anotación
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import edu.uclm.es.gramola.Dao.QueueTrackDao;
import edu.uclm.es.gramola.model.QueueTrack;
import edu.uclm.es.gramola.model.StripeTransaction;
import edu.uclm.es.gramola.services.PaymentService;
import edu.uclm.es.gramola.services.UserService;
import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("payments")
@CrossOrigin(origins = { "http://localhost:4200", "http://127.0.0.1:4200" }, allowCredentials = "true")
public class PaymentsController {

    @Autowired
    private PaymentService service;

    @Autowired
    private QueueTrackDao queueTrackDao;

    @Autowired
    private UserService userService;

    @GetMapping("/prepay")
    // NUEVO: Añadimos @RequestParam("planId") para recibir el número desde Angular
    public StripeTransaction prepay(HttpSession session, @RequestParam("planId") Long planId, @RequestParam("token") String token) {
        try {
            // NUEVO: Le pasamos ese planId al servicio para que sepa cuánto cobrar
            StripeTransaction transactionDetails = this.service.prepay(planId, token); 
            session.setAttribute("transactionDetails", transactionDetails);
            return transactionDetails;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @PostMapping("/confirm")
    public void confirm(HttpSession session, @RequestBody Map<String, Object> finalData) {
        StripeTransaction transactionDetails = (StripeTransaction) session.getAttribute("transactionDetails");
        
        if (transactionDetails == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sesión de pago caducada");
        }

        // 1. Extraemos el secret que guardamos nosotros en el prepay
        Map<String, Object> internalData = (Map<String, Object>) transactionDetails.getData();
        String sentClientSecret = internalData.get("client_secret").toString();
        String sentTransactionId = transactionDetails.getId();

        // 2. Extraemos lo que nos manda Angular (lo que viene de Stripe)
        String userToken = finalData.get("token").toString();
        String receivedTransactionId = finalData.get("transactionId").toString();
        
        Map<String, Object> paymentIntent = (Map<String, Object>) finalData.get("paymentIntent");
        String receivedClientSecret = paymentIntent.get("client_secret").toString();

        // 3. La comprobación de seguridad
        if (sentTransactionId.equals(receivedTransactionId) && sentClientSecret.equals(receivedClientSecret)) {
            this.service.confirmTransaction(userToken,sentTransactionId); 
            session.removeAttribute("transactionDetails");
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Los datos de la transacción no coinciden");
        }
    }

    @PostMapping("/bar/{barId}/intent-cancion")
    public ResponseEntity<?> crearIntencionPagoCancion( @PathVariable("barId") Long barId, @RequestBody QueueTrack trackRequest) {
        try {
            // Delegamos toda la responsabilidad al SERVICIO
            Map<String, Object> responseData = this.service.crearIntencionPagoCancion(barId, trackRequest);
            return ResponseEntity.ok(responseData);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/bar/{barId}/success-cancion")
    public ResponseEntity<?> confirmarCancionPagada( @PathVariable("barId") Long barId, @RequestParam("track_id") Long trackId) {
        try {
            // Delegamos la confirmación en la base de datos al SERVICIO
            this.service.confirmarCancionPagada(barId, trackId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/bar/{barId}/precio")
    public ResponseEntity<?> obtenerPrecioBar(@PathVariable("barId") Long barId) {
        try {
            double precio = this.service.obtenerPrecioCancion(barId);
            return ResponseEntity.ok(Map.of("precio", precio));
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    
}