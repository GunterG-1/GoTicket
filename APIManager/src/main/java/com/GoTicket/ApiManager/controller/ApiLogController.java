package com.GoTicket.ApiManager.controller;

import com.GoTicket.ApiManager.model.ApiLog;
import com.GoTicket.ApiManager.repository.ApiLogRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/logs")
@Tag(name = "Métricas y Auditoría", description = "Consultar el historial de peticiones enviadas a través del Gateway")
public class ApiLogController {

    private final ApiLogRepository logRepository;

    public ApiLogController(ApiLogRepository logRepository) {
        this.logRepository = logRepository;
    }

    @GetMapping
    @Operation(summary = "Obtener los últimos 50 registros de llamadas al Gateway")
    public List<ApiLog> getRecentLogs() {
        return logRepository.findTop50ByOrderByIdDesc();
    }

    @GetMapping("/stats")
    @Operation(summary = "Resumen de métricas generales")
    public Map<String, Object> getStats() {
        List<ApiLog> logs = logRepository.findAll();
        Map<String, Object> stats = new HashMap<>();

        long totalCalls = logs.size();
        long successCount = logs.stream().filter(l -> l.getResponseStatus() >= 200 && l.getResponseStatus() < 300).count();
        long errorCount = logs.stream().filter(l -> l.getResponseStatus() >= 400).count();
        double avgLatency = logs.stream().mapToLong(ApiLog::getExecutionTimeMs).average().orElse(0.0);

        stats.put("totalRequests", totalCalls);
        stats.put("successRequests", successCount);
        stats.put("errorRequests", errorCount);
        stats.put("averageLatencyMs", Math.round(avgLatency * 100.0) / 100.0);

        return stats;
    }
}
