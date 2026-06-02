package edu.uclm.es.gramola.model;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;

@Entity
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;
    
    private String pwd;
    
    // Campo para el nombre del bar
    private String bar;
    
    // Campo para el Client ID de la app de Spotify del bar
    private String clientId;
    
    // Campo para el Client Secret de la app de Spotify del bar
    private String clientSecret;

    // Campo fundamental para gestionar si el bar ha pagado la suscripción [cite: 40, 93]
    private boolean paid = false;

    private String gramolaCookie;

    @ManyToOne
    @JoinColumn(name = "plan_id")
    private SubscriptionPlan plan;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    private Token creationToken;

    @OneToOne(cascade = CascadeType.ALL)
    private Token resetToken;

    
    private String spotifyAccessToken;

    
    private String spotifyRefreshToken;

    
    private java.time.LocalDateTime spotifyTokenExpiresAt;

    private double precioCancion;

    

    // --- GETTERS Y SETTERS ---

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPwd() {
        return pwd;
    }

    public void setPassword(String password) {
        this.pwd = encryptPassword(password);
    }

    public String getBar() {
        return bar;
    }

    public void setBar(String bar) {
        this.bar = bar;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public boolean isPaid() {
        return paid;
    }

    public void setPaid(boolean paid) {
        this.paid = paid;
    }

    public String getGramolaCookie() {
        return gramolaCookie;
    }

    public void setGramolaCookie(String gramolaCookie) {
        this.gramolaCookie = gramolaCookie;
    }

    public SubscriptionPlan getPlan() {
        return plan;
    }

    public void setPlan(SubscriptionPlan plan) {
        this.plan = plan;
    }

    public Token getCreationToken() {
        return creationToken;
    }

    public void setCreationToken(Token creationToken) {
        this.creationToken = creationToken;
    }
    
    public Token getResetToken() {
        return resetToken;
    }

    public void setResetToken(Token resetToken) {
        this.resetToken = resetToken;
    }

    public String getSpotifyAccessToken() {
        return spotifyAccessToken;
    }

    public void setSpotifyAccessToken(String spotifyAccessToken) {
        this.spotifyAccessToken = spotifyAccessToken;
    }

    public String getSpotifyRefreshToken() {
        return spotifyRefreshToken;
    }

    public void setSpotifyRefreshToken(String spotifyRefreshToken) {
        this.spotifyRefreshToken = spotifyRefreshToken;
    }

    public java.time.LocalDateTime getSpotifyTokenExpiresAt() {
        return spotifyTokenExpiresAt;
    }

    public void setSpotifyTokenExpiresAt(java.time.LocalDateTime spotifyTokenExpiresAt) {
        this.spotifyTokenExpiresAt = spotifyTokenExpiresAt;
    }
    
    // --- LÓGICA DE NEGOCIO ---

    public String encryptPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes());
            StringBuilder hexString = new StringBuilder();
            
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error al encriptar la contraseña", e);
        }
    }

    public double getPrecioCancion() {
        return precioCancion;
    }

    public void setPrecioCancion(double precioCancion) {
        this.precioCancion = precioCancion;
    }
}