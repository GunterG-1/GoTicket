package com.GoTicket.ApiManager.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

@Entity
@Table(name = "api_routes")
public class ApiRoute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El nombre de la API es obligatorio")
    @Column(nullable = false)
    private String name;

    @NotBlank(message = "El patrón de ruta es obligatorio (ej: usuarios)")
    @Column(nullable = false, unique = true)
    private String pathPattern;

    @NotBlank(message = "La URL destino es obligatoria")
    @Column(nullable = false)
    private String targetUrl;

    private boolean enabled = true;

    private int rateLimitPerMinute = 60;

    private boolean requiresApiKey = false;

    private String description;

    private LocalDateTime createdAt = LocalDateTime.now();

    public ApiRoute() {}

    public ApiRoute(String name, String pathPattern, String targetUrl, boolean enabled, int rateLimitPerMinute, boolean requiresApiKey, String description) {
        this.name = name;
        this.pathPattern = pathPattern;
        this.targetUrl = targetUrl;
        this.enabled = enabled;
        this.rateLimitPerMinute = rateLimitPerMinute;
        this.requiresApiKey = requiresApiKey;
        this.description = description;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPathPattern() { return pathPattern; }
    public void setPathPattern(String pathPattern) { this.pathPattern = pathPattern; }

    public String getTargetUrl() { return targetUrl; }
    public void setTargetUrl(String targetUrl) { this.targetUrl = targetUrl; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public int getRateLimitPerMinute() { return rateLimitPerMinute; }
    public void setRateLimitPerMinute(int rateLimitPerMinute) { this.rateLimitPerMinute = rateLimitPerMinute; }

    public boolean isRequiresApiKey() { return requiresApiKey; }
    public void setRequiresApiKey(boolean requiresApiKey) { this.requiresApiKey = requiresApiKey; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
