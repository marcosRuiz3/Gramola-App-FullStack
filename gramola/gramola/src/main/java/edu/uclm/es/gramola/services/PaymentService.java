package edu.uclm.es.gramola.services;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;

import edu.uclm.es.gramola.Dao.QueueTrackDao;
import edu.uclm.es.gramola.Dao.SubscriptionPlanRepository;
import edu.uclm.es.gramola.Dao.UserDAO;
import edu.uclm.es.gramola.dao.StripeTransactionDAO;
import edu.uclm.es.gramola.model.QueueTrack;
import edu.uclm.es.gramola.model.StripeTransaction;
import edu.uclm.es.gramola.model.SubscriptionPlan;
import edu.uclm.es.gramola.model.User;


@Service
public class PaymentService {

    static {
        Stripe.apiKey = "sk_test_51SIV1DAExfNP6Ueb9qTYFlcjDk95GoUVz4WdLW5FpdkbArMjbSPYAdSpneG4Aqy6oLsYUcSQGTezpGMil8kun2lE006vO9tsui";
    }

    @Autowired
    private StripeTransactionDAO dao;

    @Autowired
    private SubscriptionPlanRepository planRepository;

    @Autowired
    private UserDAO userDao;

    @Autowired
    private UserService userService;

    @Autowired
    private QueueTrackDao queueTrackDao;



    public StripeTransaction prepay(Long planId, String token) throws StripeException {
    
        // 1. Buscamos al usuario usando el token para saber su email
        User user = this.userDao.findByCreationTokenId(token); 
        
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado con ese token");
        }

        // 2. Buscamos el plan
        SubscriptionPlan plan = this.planRepository.findById(planId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Plan no encontrado"));

        // 3. Creamos el intento de pago en Stripe
        long amountInCents = (long) (plan.getPrice() * 100);
        PaymentIntentCreateParams createParams = new PaymentIntentCreateParams.Builder()
                    .setCurrency("eur")
                    .setAmount(amountInCents)
                    .build();
        
        PaymentIntent intent = PaymentIntent.create(createParams);
        String transactionDetails = intent.toJson();
        
        // 4. Guardamos la transacción CON EL EMAIL y EL PLAN
        StripeTransaction st = new StripeTransaction();
        st.setData(transactionDetails);
        st.setEmail(user.getEmail());
        st.setPlan(plan); 
        
        this.dao.save(st); 
        
        return st;
    }

    // NUEVO: Añadimos el transactionId como parámetro
    public void confirmTransaction(String userToken, String transactionId) {
        
        // 1. Buscamos al usuario
        User user = userDao.findByCreationTokenId(userToken);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado para ese token");
        }

        // 2. Buscamos la transacción en la base de datos para ver qué plan tenía asociado
        StripeTransaction st = this.dao.findById(transactionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transacción no encontrada"));

        // 3. Actualizamos al usuario
        user.setPaid(true); // ¡El usuario por fin ha pagado!
        user.setPlan(st.getPlan()); // ¡AQUÍ LE ASIGNAMOS EL PLAN!
        
        userDao.save(user);
    }

    // ---------------------------------------------------------
    // 🎵 LÓGICA DE NEGOCIO: PAGOS DE CANCIONES
    // ---------------------------------------------------------
    
    public Map<String, Object> crearIntencionPagoCancion(Long barId, QueueTrack trackRequest) throws Exception {
        // 1. Guardamos la canción en la BD en estado "UNPAID"
        trackRequest.setStatus("UNPAID");
        User bar = this.userService.findById(barId);
        trackRequest.setBar(bar);
        QueueTrack savedTrack = this.queueTrackDao.save(trackRequest);
        Double precio = bar.getPrecioCancion();
        long amountInCents = Math.round(precio * 100);

        // 2. Creamos la intención de cobro en Stripe (1,00 €)
        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
            .setAmount(amountInCents) 
            .setCurrency("eur")
            .setDescription("Canción VIP: " + trackRequest.getTrackName())
            .build();

        PaymentIntent intent = PaymentIntent.create(params);

        // 3. EL TOQUE MAESTRO: Guardamos el "ticket" en nuestra base de datos
        StripeTransaction transaction = new StripeTransaction();
        transaction.setTransactionType("SONG_REQUEST");
        transaction.setTrack(savedTrack);
        transaction.setData(intent.toJson()); // Guardamos la respuesta cruda de Stripe por seguridad
        
        // Como el cliente es anónimo, puedes dejar el email en null o poner uno genérico:
        transaction.setEmail("cliente_anonimo_bar_" + barId); 
        
        this.dao.save(transaction); // ¡Guardado para contabilidad!

        // 4. Preparamos el paquete de datos para Angular
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("clientSecret", intent.getClientSecret());
        responseData.put("trackId", savedTrack.getId());
        
        return responseData;
    }

    public void confirmarCancionPagada(Long barId, Long trackId) throws Exception {
        // 1. Buscamos la canción en la base de datos
        QueueTrack track = this.queueTrackDao.findById(trackId).orElse(null);
        
        if (track != null && "UNPAID".equals(track.getStatus())) {
            // 2. ¡La magia VIP! Cambiamos a PENDING y marcamos como pagada
            track.setStatus("PENDING");
            track.setPaid(true);
            this.queueTrackDao.save(track);
        } else {
            throw new Exception("La canción ya fue pagada o no existe.");
        }
    }

    public double obtenerPrecioCancion(Long barId) throws Exception {
        User bar = this.userService.findById(barId);
        return bar.getPrecioCancion();
    }
}