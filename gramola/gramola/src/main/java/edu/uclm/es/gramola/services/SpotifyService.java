package edu.uclm.es.gramola.services;


import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import edu.uclm.es.gramola.Dao.QueueTrackDao;
import edu.uclm.es.gramola.Dao.UserDAO;
import edu.uclm.es.gramola.model.QueueTrack;
import edu.uclm.es.gramola.model.User;

@Service
public class SpotifyService {

    
    
    private String clientId;


    private String clientSecret;

    @Autowired
    private QueueTrackDao queueTrackDao; // Para gestionar la cola de canciones del Bar


    // 2. Necesitamos el UserDao para guardar los tokens en el Bar correcto
    @Autowired
    private final UserDAO userDao;

    private final RestTemplate restTemplate; // Herramienta para hacer peticiones HTTP

    // La URL de redirección sí la podemos dejar fija, ya que tu Angular siempre está en el 4200
    private final String REDIRECT_URI = "http://127.0.0.1:4200/callback";

    public SpotifyService(UserDAO userDao) {
        this.userDao = userDao;
        this.restTemplate = new RestTemplate();
    }



    public void vincularCuenta(String code, String gramolaCookie) {
        
        // 1. Buscamos al Bar usando su sesión
        User bar = userDao.findByGramolaCookie(gramolaCookie);
        if (bar == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sesión inválida");
        }

        // 2. Extraemos SUS credenciales de Spotify (las que puso en el registro)
        String clientId = bar.getClientId();
        String clientSecret = bar.getClientSecret();

        if (clientId == null || clientSecret == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El bar no configuró sus credenciales de Spotify");
        }

        // 3. Preparamos la Autorización para Spotify (Base64 de clientId:clientSecret)
        String authHeader = "Basic " + Base64.getEncoder().encodeToString((clientId + ":" + clientSecret).getBytes());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("Authorization", authHeader);

        // 4. Preparamos el cuerpo de la petición (lo que Spotify nos exige)
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("code", code);
        body.add("redirect_uri", REDIRECT_URI);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        // 5. HACEMOS LA LLAMADA A SPOTIFY
        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    "https://accounts.spotify.com/api/token", 
                    request, 
                    Map.class
            );

            // 6. Si Spotify nos dice que OK, guardamos los tokens en la BD
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                
                bar.setSpotifyAccessToken((String) responseBody.get("access_token"));
                bar.setSpotifyRefreshToken((String) responseBody.get("refresh_token"));
                
                // Spotify nos dice cuántos segundos dura (suele ser 3600 = 1 hora)
                Integer expiresIn = (Integer) responseBody.get("expires_in");
                LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(expiresIn);
                bar.setSpotifyTokenExpiresAt(expiresAt);

                // Guardamos el usuario actualizado
                userDao.save(bar);
            }

        } catch (Exception e) {
            // Si el código caducó o hay algún error de red
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Error al vincular con Spotify: " + e.getMessage());
        }
    }

    public Object searchTracks(String accessToken, String query) {
        RestTemplate restTemplate = new RestTemplate();

        // 1. Preparamos la cabecera de la petición con tu Token
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken); 
        headers.set("Accept", "application/json");

        // Metemos las cabeceras en un "sobre"
        HttpEntity<String> entity = new HttpEntity<>(headers);

        // 2. Construimos la URL oficial de la API de Spotify
        // Reemplazamos los espacios por "%20" (ej: "Estopa Como Camaron" -> "Estopa%20Como%20Camaron")
        String safeQuery = query.replace(" ", "%20");
        
        // Buscamos de tipo 'track' (canción) y pedimos solo 10 resultados para no saturar la pantalla
        String url = "https://api.spotify.com/v1/search?q=" + safeQuery + "&type=track&limit=10";

        // 3. Disparamos la petición GET hacia Spotify
        ResponseEntity<Object> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                Object.class // Queremos que nos devuelva el JSON tal cual
        );

        // 4. Devolvemos la lista de canciones al Controlador (y el Controlador a Angular)
        return response.getBody();
    }

    public void addTrackToQueue(User bar, String spotifyUri, String trackName, String artist, String albumImageUrl) {
        QueueTrack track = new QueueTrack();
        track.setBar(bar);
        track.setSpotifyUri(spotifyUri);
        track.setTrackName(trackName);
        track.setArtist(artist);
        track.setAlbumImageUrl(albumImageUrl);
        
        // createdAt y status se ponen solos por tu constructor 🪄
        this.queueTrackDao.save(track);
        
    }

    public List<QueueTrack> getQueueForBar(User bar) {
        // 1. Definimos los estados que queremos rescatar de la base de datos
        List<String> estadosVivos = Arrays.asList("PLAYING", "PAUSED", "PENDING");
        
        // 2. Se los pasamos al nuevo método del DAO
        return this.queueTrackDao.obtenerColaPerfecta(bar.getId(), estadosVivos);
    }

    public String getSpotifyAccessToken(User bar) {
        // Aquí podrías implementar la lógica para refrescar el token si ha caducado
        return bar.getSpotifyAccessToken();
    }

    

    public void changeTrackState(String trackUri, String newStatus) {
        // Buscamos la canción por URI, asegurándonos de que NO sea una que ya sonó en el pasado
        QueueTrack track = this.queueTrackDao.findFirstBySpotifyUriAndStatusNot(trackUri, "FINISHED");
        
        if (track != null) {
            track.setStatus(newStatus);
            this.queueTrackDao.save(track);
        }
    }

    public void skipToNextTrack(Long barId) { //  Exigimos el ID del Bar
        List<String> vivos = Arrays.asList("PLAYING", "PAUSED");
        QueueTrack actual = this.queueTrackDao.findFirstByBar_IdAndStatusIn(barId, vivos);
        
        if (actual != null) {
            actual.setStatus("FINISHED");
            this.queueTrackDao.save(actual);
            System.out.println("⏭️ Canción saltada. Guardada en el historial.");
        }
    }

    public void previousTrack(Long barId) { //  Exigimos el ID del Bar
        // 1. La canción actual vuelve a la sala de espera
        List<String> vivos = Arrays.asList("PLAYING", "PAUSED");
        QueueTrack actual = this.queueTrackDao.findFirstByBar_IdAndStatusIn(barId, vivos);
        if (actual != null) {
            actual.setStatus("PENDING");
            this.queueTrackDao.save(actual);
        }

        // 2. Resucitamos la última canción que terminó (¡Ojo, usando UpdatedAt!)
        QueueTrack anterior = this.queueTrackDao.findFirstByBar_IdAndStatusOrderByUpdatedAtDesc(barId, "FINISHED");
        if (anterior != null) {
            // La ponemos en PLAYING directamente. 
            // Así ignoramos a los VIPs y forzamos a que sea la número 1 indiscutible.
            anterior.setStatus("PLAYING"); 
            this.queueTrackDao.save(anterior);
            System.out.println("⏮️ Canción anterior resucitada y forzada a sonar.");
        }
    }

    public List<QueueTrack> getHistoryForBar(User bar) {
        return this.queueTrackDao.findByBar_IdAndStatusOrderByCreatedAtDesc(bar.getId(), "FINISHED");
    }
}