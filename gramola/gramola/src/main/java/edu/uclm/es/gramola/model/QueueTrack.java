package edu.uclm.es.gramola.model;


import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "queue_tracks")
public class QueueTrack {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relación con el Bar (User)
    @ManyToOne
    @JoinColumn(name = "bar_id", nullable = false)
    @JsonIgnore // Para evitar ciclos en la serialización JSON
    private User bar;

    @Column(nullable = false)
    private String spotifyUri; // Ejemplo: spotify:track:4iV5W...

    private String trackName;
    private String artist;

    @Column(name = "album_image_url") // Lo llamamos así en la BD
    private String albumImageUrl;
    
    // Estado: "PENDING", "PLAYING", "FINISHED"
    @Column
    private String status;

    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "is_paid")
    private boolean isPaid = false;

    // Constructor que inicializa la fecha
    public QueueTrack() {
        this.createdAt = LocalDateTime.now();
        this.status = "PENDING";
    }

    // Getters y Setters...
    public Long getId() {
        return id;
    }

    public User getBar() {
        return bar;
    }

    public void setBar(User bar) {
        this.bar = bar;
    }

    public String getSpotifyUri() {
        return spotifyUri;
    }

    public void setSpotifyUri(String spotifyUri) {
        this.spotifyUri = spotifyUri;
    }

    public String getTrackName() {
        return trackName;
    }

    public void setTrackName(String trackName) {
        this.trackName = trackName;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getAlbumImageUrl() {
        return albumImageUrl;
    }

    public void setAlbumImageUrl(String albumImageUrl) {
        this.albumImageUrl = albumImageUrl;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public boolean isPaid() {
        return isPaid;
    }

    public void setPaid(boolean isPaid) {
        this.isPaid = isPaid;
    }

    // Este método se ejecuta automáticamente justo antes de hacer un INSERT en la BD
    @PrePersist
    protected void onCreate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Este método se ejecuta automáticamente justo antes de hacer un UPDATE en la BD
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}