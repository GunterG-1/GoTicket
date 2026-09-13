package com.GoTicket.ApiManager.service;

import com.GoTicket.ApiManager.model.ApiLog;
import com.GoTicket.ApiManager.model.ApiRoute;
import com.GoTicket.ApiManager.repository.ApiLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.Enumeration;
import java.util.stream.Collectors;

@Service
public class ProxyService {

    private final ApiLogRepository apiLogRepository;
    private final RestTemplate restTemplate;

    public ProxyService(ApiLogRepository apiLogRepository) {
        this.apiLogRepository = apiLogRepository;
        this.restTemplate = new RestTemplate();
    }

    public ResponseEntity<byte[]> forwardRequest(ApiRoute route, String remainingPath, HttpServletRequest request, String apiKeyUsed) {
        long startTime = System.currentTimeMillis();
        int statusCode = 500;
        byte[] responseBody = new byte[0];
        HttpHeaders responseHeaders = new HttpHeaders();

        String targetUrl = route.getTargetUrl();
        if (!targetUrl.endsWith("/") && remainingPath != null && !remainingPath.startsWith("/") && !remainingPath.isEmpty()) {
            targetUrl += "/";
        }
        if (remainingPath != null) {
            targetUrl += remainingPath;
        }

        if (request.getQueryString() != null) {
            targetUrl += "?" + request.getQueryString();
        }

        try {
            HttpMethod method = HttpMethod.valueOf(request.getMethod());
            HttpHeaders headers = extractHeaders(request);
            byte[] body = extractBody(request);

            HttpEntity<byte[]> httpEntity = new HttpEntity<>(body, headers);
            ResponseEntity<byte[]> response = restTemplate.exchange(
                    URI.create(targetUrl),
                    method,
                    httpEntity,
                    byte[].class
            );

            statusCode = response.getStatusCode().value();
            responseHeaders.putAll(response.getHeaders());
            responseBody = response.getBody() != null ? response.getBody() : new byte[0];

            return new ResponseEntity<>(responseBody, responseHeaders, response.getStatusCode());

        } catch (HttpStatusCodeException ex) {
            statusCode = ex.getStatusCode().value();
            responseHeaders.putAll(ex.getResponseHeaders() != null ? ex.getResponseHeaders() : new HttpHeaders());
            responseBody = ex.getResponseBodyAsByteArray();
            return new ResponseEntity<>(responseBody, responseHeaders, ex.getStatusCode());
        } catch (Exception ex) {
            statusCode = HttpStatus.BAD_GATEWAY.value();
            String errorJson = "{\"error\": \"Proxy error: " + ex.getMessage().replace("\"", "'") + "\"}";
            responseHeaders.setContentType(MediaType.APPLICATION_JSON);
            return new ResponseEntity<>(errorJson.getBytes(), responseHeaders, HttpStatus.BAD_GATEWAY);
        } finally {
            long executionTimeMs = System.currentTimeMillis() - startTime;
            ApiLog log = new ApiLog(
                    route.getPathPattern(),
                    request.getRequestURI(),
                    targetUrl,
                    request.getMethod(),
                    statusCode,
                    executionTimeMs,
                    request.getRemoteAddr(),
                    apiKeyUsed
            );
            apiLogRepository.save(log);
        }
    }

    private HttpHeaders extractHeaders(HttpServletRequest request) {
        HttpHeaders headers = new HttpHeaders();
        Enumeration<String> headerNames = request.getHeaderNames();
        if (headerNames != null) {
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                if (!headerName.equalsIgnoreCase("host") &&
                    !headerName.equalsIgnoreCase("content-length") &&
                    !headerName.equalsIgnoreCase("connection")) {
                    headers.put(headerName, Collections.list(request.getHeaders(headerName)));
                }
            }
        }
        return headers;
    }

    private byte[] extractBody(HttpServletRequest request) throws IOException {
        if ("POST".equalsIgnoreCase(request.getMethod()) ||
            "PUT".equalsIgnoreCase(request.getMethod()) ||
            "PATCH".equalsIgnoreCase(request.getMethod())) {
            try (BufferedReader reader = request.getReader()) {
                String bodyStr = reader.lines().collect(Collectors.joining(System.lineSeparator()));
                return bodyStr.getBytes();
            }
        }
        return new byte[0];
    }
}
