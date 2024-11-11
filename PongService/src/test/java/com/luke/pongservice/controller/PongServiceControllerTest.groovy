package com.luke.pongservice.controller

import io.github.bucket4j.Bucket
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import spock.lang.Specification

class PongServiceControllerTest extends Specification {

    PongServiceController controller
    Bucket mockBucket

    def setup() {
        mockBucket = Mock(Bucket)
        controller = new PongServiceController()
    }

    def "Test Pong Respond Success"() {
        given:
        mockBucket.tryConsume(1) >> true

        when:
        Mono<ResponseEntity<String>> response = controller.respond("Hello")

        then:
        StepVerifier.create(response)
                .expectNextMatches {
                    it.statusCode == HttpStatus.OK
                }.verifyComplete()
    }

    def "Test Pong Responds  429"() {
        given:
        def responses = (1..3).collect {
            controller.respond("Hello")
        }

        expect:
        StepVerifier.create(Flux.merge(responses))
                // 第一个成功请求应该返回 200 OK 和 Pong
                .expectNextMatches { response ->
                    response.body == "Pong" && response.statusCode == HttpStatus.OK
                }
                // 第二个请求应该TOO_MANY_REQUESTS
                .expectNextMatches { response ->
                    response.body == "Too Many Requests" && response.statusCode == HttpStatus.TOO_MANY_REQUESTS
                }
    }
}
