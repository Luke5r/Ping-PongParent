package com.luke.pingclient.controller

import com.luke.pingclient.config.FileLockLimiter
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class PingServiceTest extends Specification {

    def limiterMock = Mock(FileLockLimiter)
    def webClientMock = Mock(WebClient)
    def requestHeadersUriSpec = Mock(WebClient.RequestHeadersUriSpec)
    def responseSpec = Mock(WebClient.ResponseSpec)
    def webClientBuilderMock = Mock(WebClient.Builder)

    PingService pingService

    def "Test FileLockLimiter --> multiple microservices on different ports"() {
        given: "Mock WebClient and shared FileLockLimiter"
        limiterMock = new FileLockLimiter()
        webClientBuilderMock.baseUrl(_ as String) >> webClientBuilderMock
        webClientBuilderMock.build() >> webClientMock
        // 配置 WebClient 链式调用
        webClientMock.get() >> requestHeadersUriSpec
        requestHeadersUriSpec.uri(_) >> requestHeadersUriSpec
        requestHeadersUriSpec.retrieve() >> responseSpec
        responseSpec.toEntity(String) >> Mono.just(new ResponseEntity<>("World", HttpStatus.OK))
        pingService = new PingService(webClientBuilderMock, limiterMock)


        when: "Each service instance calls sendPing concurrently"
        def pool = Executors.newFixedThreadPool(3)
        def responses = (1..3).collect {
            Mono.defer {
                 pingService.sendPing()
                        .onErrorResume {
                            Mono.just(new ResponseEntity<>("Rate limited", HttpStatus.TOO_MANY_REQUESTS))
                        }
            }.subscribeOn(Schedulers.fromExecutor(pool))
        }

        then:
        def conditions = new PollingConditions(timeout: 1)
        conditions.eventually {
            responses.collect { it.block() }.count {
                it.statusCode == HttpStatus.OK
            } >= 2

            responses.collect { it.block() }.count {
                it.statusCode == HttpStatus.TOO_MANY_REQUESTS
            } > 1
        }
        cleanup:
        pool.shutdown()
        pool.awaitTermination(1, TimeUnit.SECONDS)
    }

}