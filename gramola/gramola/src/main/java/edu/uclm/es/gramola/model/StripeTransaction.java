package edu.uclm.es.gramola.model;

import java.util.Map;

import org.json.JSONObject;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne; // 🔴 Importamos esto

@Entity
public class StripeTransaction {
    
    @Id 
    @Column(length = 36)
    private String id;

    @Column(columnDefinition = "json")
    private String data;

    private String email;

    // Relación con el plan elegido (nullable = true por si es el pago de una canción)
    @ManyToOne
    @JoinColumn(name = "plan_id", nullable = true) 
    private SubscriptionPlan plan;

    //  Relación con la canción pedida (nullable = true por si es una suscripción)
    @OneToOne
    @JoinColumn(name = "track_id", nullable = true)
    private QueueTrack track;

    //  Para filtrar rápido ("SUBSCRIPTION" o "SONG_REQUEST")
    private String transactionType;

    public StripeTransaction() {
        this.id = java.util.UUID.randomUUID().toString();
    }

    // --- GETTERS Y SETTERS ---

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Map<String, Object> getData() {
        return new JSONObject(this.data).toMap();
    }

    public void setData(String data) {
        this.data = data;
    }

    public void setData(JSONObject jsoData) {
        this.data = jsoData.toString();
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
    
    public SubscriptionPlan getPlan() {
        return plan;
    }

    public void setPlan(SubscriptionPlan plan) {
        this.plan = plan;
    }

    // --- GETTERS Y SETTERS NUEVOS ---

    public QueueTrack getTrack() {
        return track;
    }

    public void setTrack(QueueTrack track) {
        this.track = track;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }
}