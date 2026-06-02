package edu.uclm.es.gramola.http;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import edu.uclm.es.gramola.model.QueueTrack;
import edu.uclm.es.gramola.model.User;
import edu.uclm.es.gramola.services.SpotifyService;
import edu.uclm.es.gramola.services.UserService;

@RestController
@RequestMapping("/api/spotify")
@CrossOrigin(
    origins = { "http://localhost:4200", "http://127.0.0.1:4200" }, 
    allowCredentials = "true", 
    allowedHeaders = "*",
    //  Le decimos a Spring explícitamente: "Acepta GET, POST, el PUT que necesitamos y las preguntas OPTIONS"
    methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.OPTIONS}
)
public class SpotifyController {

    @Autowired
    private SpotifyService spotifyService;

    @Autowired
    private UserService userService; // Añadimos el servicio de usuarios

    // ==========================================
    // 1. ENDPOINT: VINCULAR CUENTA 
    // ==========================================
    @PostMapping("/vincular")
    public void vincularCuenta(
            @RequestBody Map<String, String> body,
            @RequestHeader(value = "Gramola-Cookie", required = false) String cookie) { 

        String code = body.get("code");

        if (code == null || code.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Falta el código de autorización de Spotify");
        }
        
        if (cookie == null || cookie.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Falta la cookie de sesión");
        }

        this.spotifyService.vincularCuenta(code, cookie);
    }

    // ==========================================
    // 2. ENDPOINT: BUSCAR CANCIONES 
    // ==========================================
    @GetMapping("/search")
    public ResponseEntity<?> searchTracks(
            @RequestHeader(value = "Gramola-Cookie", required = false) String cookie,
            @RequestParam("q") String query) { 

        // 1. Comprobamos la llave
        if (cookie == null || cookie.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Falta la cookie de sesión");
        }

        // 2. Buscamos al usuario en la base de datos
        User user = userService.findByGramolaCookie(cookie); 
        
        // 3. Validamos que exista y tenga token de Spotify
        if (user == null || user.getSpotifyAccessToken() == null || user.getSpotifyAccessToken().isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                 .body("Error: Usuario no autorizado o Spotify no vinculado.");
        }

        try {
            // 4. Llamamos al servicio pasándole el token y lo que el usuario ha escrito
            Object resultados = spotifyService.searchTracks(user.getSpotifyAccessToken(), query);
            return ResponseEntity.ok(resultados);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body("Error al comunicarse con Spotify: " + e.getMessage());
        }
    }

    @PostMapping("/add-to-queue")
    public ResponseEntity<?> addToQueue(
            @RequestHeader("Gramola-Cookie") String cookie,
            @RequestBody Map<String, String> payload) {

        // 1. Buscamos de quién es el bar usando la llave (necesitas tener el userDao inyectado)
        User bar = this.userService.findByGramolaCookie(cookie);
        if (bar == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // 2. Sacamos los datos de la canción
        String spotifyUri = payload.get("spotifyUri");
        String trackName = payload.get("trackName");
        String artist = payload.get("artist");
        String albumImageUrl = payload.get("albumImageUrl");

        // 3. Mandamos al servicio a guardar
        this.spotifyService.addTrackToQueue(bar, spotifyUri, trackName, artist, albumImageUrl);

        // 4. Devolvemos un OK a Angular
        return ResponseEntity.ok(Map.of("message", "¡Canción añadida a la cola!"));
    }

    @GetMapping("/queue")
    public ResponseEntity<?> getQueue(@RequestHeader("Gramola-Cookie") String cookie) {
        // 1. Identificamos al bar
        User bar = this.userService.findByGramolaCookie(cookie);
        if (bar == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // 2. Pedimos la lista al servicio
        List<QueueTrack> cola = this.spotifyService.getQueueForBar(bar);


        // 3. Se la enviamos a Angular
        return ResponseEntity.ok(cola);
    }

    @GetMapping("/token")
    public ResponseEntity<?> getAccessToken(@RequestHeader("Gramola-Cookie") String cookie) {
        // 1. Identificamos al bar
        User bar = this.userService.findByGramolaCookie(cookie);
        
        // 2. Si no existe o no tiene token, le damos un 401
        if (bar == null || bar.getSpotifyAccessToken() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String token = this.spotifyService.getSpotifyAccessToken(bar); // Aquí podrías refrescar el token si ha caducado

        // 3. Le mandamos el token actual en un JSON pequeño
        return ResponseEntity.ok(Map.of("token", token));
    }

    

    @PutMapping("/change-state")
    public ResponseEntity<?> changeTrackState(
            @RequestHeader("Gramola-Cookie") String cookie, 
            @RequestBody Map<String, String> payload) {

        // 1. Identificamos al bar
        User bar = this.userService.findByGramolaCookie(cookie);
        if (bar == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // 2. Extraemos el ID de la canción y el nuevo estado
        String trackUri = payload.get("spotifyUri");
        String nuevoEstado = payload.get("nuevoEstado");

        // 3. Le decimos al servicio que actualice el estado de esa canción
        this.spotifyService.changeTrackState(trackUri, nuevoEstado);

        return ResponseEntity.ok(Map.of("mensaje", "Estado actualizado"));
    }

    @PostMapping("/next")
    public ResponseEntity<?> skipNext(@RequestHeader("Gramola-Cookie") String cookie) {
        User bar = this.userService.findByGramolaCookie(cookie);
        if (bar == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        this.spotifyService.skipToNextTrack(bar.getId());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/previous")
    public ResponseEntity<?> skipPrevious(@RequestHeader("Gramola-Cookie") String cookie) {
        User bar = this.userService.findByGramolaCookie(cookie);
        if (bar == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        this.spotifyService.previousTrack(bar.getId());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/history")
    public ResponseEntity<?> getHistory(@RequestHeader("Gramola-Cookie") String cookie) {
        // 1. Identificamos al bar (como siempre)
        User bar = this.userService.findByGramolaCookie(cookie);
        if (bar == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        List<QueueTrack> history = this.spotifyService.getHistoryForBar(bar);
        
        return ResponseEntity.ok(history);
    }
}