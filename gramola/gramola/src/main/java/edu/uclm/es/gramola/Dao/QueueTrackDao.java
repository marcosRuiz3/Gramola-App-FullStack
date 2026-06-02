package edu.uclm.es.gramola.Dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import edu.uclm.es.gramola.model.QueueTrack;

public interface QueueTrackDao extends JpaRepository<QueueTrack, Long> {
    


    @Query("SELECT q FROM QueueTrack q WHERE q.bar.id = :barId AND q.status IN :statuses " +
           "ORDER BY " +
           "CASE WHEN q.status = 'PLAYING' THEN 1 " +
           "WHEN q.status = 'PAUSED' THEN 2 " +
           "ELSE 3 END ASC, " + // 1º: Manda lo que está sonando arriba del todo
           "q.isPaid DESC, " +  // 2º: Si están en PENDING, los pagados van primero
           "q.createdAt ASC")   // 3º: Si hay varios pagados (o varios gratis), por orden de llegada
    List<QueueTrack> obtenerColaPerfecta(@Param("barId") Long barId, @Param("statuses") List<String> statuses);

    // Busca la canción viva de UN BAR en concreto
    QueueTrack findFirstByBar_IdAndStatusIn(Long barId, List<String> statuses);

    // Busca la última canción que terminó en ESE BAR (ordenada por cuándo se actualizó a FINISHED)
    QueueTrack findFirstByBar_IdAndStatusOrderByUpdatedAtDesc(Long barId, String status);
        
    // Busca la primera coincidencia que esté en "PENDING" para ponerlas a "PLAYING" (para no pisar las ya "FINISHED")
    QueueTrack findFirstBySpotifyUriAndStatus(String spotifyUri, String status);

    // Busca la primera coincidencia que NO haya terminado ya
    QueueTrack findFirstBySpotifyUriAndStatusNot(String spotifyUri, String status);

    // Busca la última canción que terminó (ordenada de más reciente a más antigua)
    QueueTrack findFirstByStatusOrderByCreatedAtDesc(String status);

    // Y asegúrate de tener este para encontrar la actual:
    QueueTrack findFirstByStatusIn(List<String> statuses);

    // Busca las canciones terminadas y las ordena de más reciente a más antigua
    List<QueueTrack> findByBar_IdAndStatusOrderByCreatedAtDesc(Long barId, String status);

    // Para buscar si una canción ya está en la cola "PENDIENTE" y no repetirla
    boolean existsByBar_IdAndSpotifyUriAndStatus(Long barId, String spotifyUri, String status);
}