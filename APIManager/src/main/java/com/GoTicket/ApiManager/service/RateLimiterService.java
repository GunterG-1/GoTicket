package com.GoTicket.ApiManager.service;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class RateLimiterService {

    private static class RequestWindow {
        long windowStartMs;
        AtomicInteger count;

        RequestWindow(long windowStartMs) {
            this.windowStartMs = windowStartMs;
            this.count = new AtomicInteger(1);
        }
    }

    private final Map<String, RequestWindow> requestCounts = new ConcurrentHashMap<>();

    public boolean isAllowed(String key, int maxRequestsPerMinute) {
        if (maxRequestsPerMinute <= 0) {
            return true;
        }

        long now = System.currentTimeMillis();
        long currentWindow = now / 60000;

        RequestWindow window = requestCounts.compute(key, (k, existingWindow) -> {
            if (existingWindow == null || (existingWindow.windowStartMs / 60000) != currentWindow) {
                return new RequestWindow(now);
            } else {
                existingWindow.count.incrementAndGet();
                return existingWindow;
            }
        });

        return window.count.get() <= maxRequestsPerMinute;
    }
}
