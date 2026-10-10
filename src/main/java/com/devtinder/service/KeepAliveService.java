package com.devtinder.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Service
public class KeepAliveService {

    private static final Logger log = LoggerFactory.getLogger(KeepAliveService.class);
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Value("${app.keep-alive.url:}")
    private String keepAliveUrl;

    @Value("${server.port:8080}")
    private int port;

    // Ping every 12 minutes (720,000 ms) to keep free-tier instances (e.g. Render) active
    @Scheduled(fixedRate = 720000, initialDelay = 60000)
    public void pingSelf() {
        String target = (keepAliveUrl != null && !keepAliveUrl.isBlank())
                ? keepAliveUrl
                : "http://localhost:" + port + "/api/health";

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(target))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            log.info("Keep-alive ping to [{}] returned status [{}]", target, response.statusCode());
        } catch (Exception e) {
            log.debug("Keep-alive ping to [{}] failed (non-critical): {}", target, e.getMessage());
        }
    }
}
