package com.luke.pongservice.controller;


import io.github.bucket4j.Bucket;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Slf4j
@RestController
@RequestMapping("/pong")
public class PongServiceController {

    private final Bucket rateLimiter;

    public PongServiceController() {
        this.rateLimiter = Bucket.builder().addLimit(limit -> limit.capacity(1).refillGreedy(1, Duration.ofSeconds(1)))
                .build();
    }

    @GetMapping("/response")
    public Mono<ResponseEntity<String>> respond(@RequestParam("say")  String hello) {
        log.info("Ping Client say: " + hello);
        if (rateLimiter.tryConsume(1)) {
            log.info("Success & response: " + "World");
            return Mono.just(ResponseEntity.ok("World"));
        } else {
            log.info("Too Many Requests");
            return Mono.just(ResponseEntity.status(429).body("Too Many Requests"));
        }
    }
}