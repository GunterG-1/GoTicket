package com.GoTicket.ApiManager.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

@Entity
@Table(name = "api_keys")
public class ApiKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "La clave de la API es obligatoria")
    @Column(nullable = false, unique = true)
    private String keyString;

    @NotBlank(message = "El nombre del propietario/cliente es obligatorio")
    private String ownerName;

    private boolean active = true;

    private int rateLimitPerMinute = 100;

    private LocalDateTime createdAt = LocalDateTime.now();

    public ApiKey() {}

    public ApiKey(String keyString, String ownerName, boolean active, int rateLimitPerMinute) {
        this.keyString = keyString;
        this.ownerName = ownerName;
        this.active = active;
        this.rateLimitPerMinute = rateLimitPerMinute;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getKeyString() { return keyString; }
    public void setKeyString(String keyString) { this.keyString = keyString; }

    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public int getRateLimitPerMinute() { return rateLimitPerMinute; }
    public void setRateLimitPerMinute(int rateLimitPerMinute) { this.rateLimitPerMinute = rateLimitPerMinute; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
