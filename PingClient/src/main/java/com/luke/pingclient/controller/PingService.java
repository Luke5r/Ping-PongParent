package com.luke.pingclient.controller;

import com.luke.pingclient.config.FileLockLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@RestController
public class PingService {

    private final WebClient webClient;
    private final FileLockLimiter fileLockLimiter;

    public PingService(WebClient.Builder builder, FileLockLimiter limiter) {
        this.webClient = builder.baseUrl("http://localhost:8081/pong").build();
        this.fileLockLimiter = limiter;
    }


    @GetMapping("/ping")
    public Mono<ResponseEntity<String>> sendPing() {
        if (fileLockLimiter.tryAcquireLock()) {
            log.info("winner is: " + ProcessHandle.current().pid() + " -- time: " +
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS")));
            return webClient.get()
                    .uri("/response?say=Hello")
                    .retrieve()
                    .toEntity(String.class)
                    .doOnSuccess(response -> log.info("Received: {}", response.getBody()))
                    .doOnError(error -> log.error("Failed: {}", error.getMessage()))
                    .onErrorResume(error ->  {
                        log.error("Error occurred: {}", error.getMessage(), error);
                        return Mono.just(ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(error.getMessage()));
                    });
        } else {
            log.warn("Skipped request due to global rate limiting");
            return Mono.just(ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body("Rate limit exceeded (2 RPS)"));
        }
    }
}
