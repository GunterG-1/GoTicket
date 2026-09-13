package com.GoTicket.ApiManager.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "api_logs")
public class ApiLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String routePattern;
    private String requestPath;
    private String targetUrl;
    private String httpMethod;
    private int responseStatus;
    private long executionTimeMs;
    private String clientIp;
    private String apiKeyUsed;
    private LocalDateTime timestamp = LocalDateTime.now();

    public ApiLog() {}

    public ApiLog(String routePattern, String requestPath, String targetUrl, String httpMethod, int responseStatus, long executionTimeMs, String clientIp, String apiKeyUsed) {
        this.routePattern = routePattern;
        this.requestPath = requestPath;
        this.targetUrl = targetUrl;
        this.httpMethod = httpMethod;
        this.responseStatus = responseStatus;
        this.executionTimeMs = executionTimeMs;
        this.clientIp = clientIp;
        this.apiKeyUsed = apiKeyUsed;
        this.timestamp = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getRoutePattern() { return routePattern; }
    public void setRoutePattern(String routePattern) { this.routePattern = routePattern; }

    public String getRequestPath() { return requestPath; }
    public void setRequestPath(String requestPath) { this.requestPath = requestPath; }

    public String getTargetUrl() { return targetUrl; }
    public void setTargetUrl(String targetUrl) { this.targetUrl = targetUrl; }

    public String getHttpMethod() { return httpMethod; }
    public void setHttpMethod(String httpMethod) { this.httpMethod = httpMethod; }

    public int getResponseStatus() { return responseStatus; }
    public void setResponseStatus(int responseStatus) { this.responseStatus = responseStatus; }

    public long getExecutionTimeMs() { return executionTimeMs; }
    public void setExecutionTimeMs(long executionTimeMs) { this.executionTimeMs = executionTimeMs; }

    public String getClientIp() { return clientIp; }
    public void setClientIp(String clientIp) { this.clientIp = clientIp; }

    public String getApiKeyUsed() { return apiKeyUsed; }
    public void setApiKeyUsed(String apiKeyUsed) { this.apiKeyUsed = apiKeyUsed; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
