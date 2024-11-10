import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.reactive.function.client.WebClient
import spock.lang.Specification

import java.util.concurrent.Executors
import reactor.core.publisher.Mono

import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PingServiceRealRequsetTests extends Specification {

    @Autowired
    WebClient webClient

    def "Test FileLockLimiter --> multiple microservices on different ports"() {
        given: "Three Dif Port"
        List<Mono<ResponseEntity<String>>> requestMonos = new ArrayList<>()
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(3);
        when:
        for (int i = 0; i < 3; i++) {
            final int requestId = i;
            int port = 8082+i
            scheduler.schedule(() -> {
                createWebClient(port).get()
                        .uri("/ping")
                        .retrieve()
                        .toEntity(String.class)
                        .onErrorResume {
                            Mono.just(new ResponseEntity<>("Rate limited by Ping Or Pong", HttpStatus.TOO_MANY_REQUESTS))
                        }.subscribe(response -> {
                    System.out.println("Response for request " + requestId + ": " + response);
                    requestMonos.add(Mono.just(response))
                });
                // 可选：记录请求和响应的详细信息
                System.out.println("Request " + requestId + " completed." + "requestServerPort: " + port + " time: " + System.currentTimeMillis());
            }, i * 500, TimeUnit.MILLISECONDS);
        }


        Thread.sleep(1500);

        scheduler.shutdown();

        then: "check response statusCode&body"

        assert requestMonos.size() == 3 : "Expected 3 responses, but got ${requestMonos.size()}"

        requestMonos.each { responseMono ->
            responseMono.subscribe { response ->

                System.out.println("Received response: " + response)

                if (response.statusCode == HttpStatus.OK) {
                    assert response.body == "World" : "Expected 'Success' in the response body, but got ${response.body}"
                } else if (response.statusCode == HttpStatus.TOO_MANY_REQUESTS) {
                    assert response.body == "Rate limited by Ping Or Pong" : "Expected 'Too Many Requests,Rate limited by Ping Or Pong' in the response body, but got ${response.body}"
                } else if (response.statusCode == HttpStatus.NOT_FOUND) {
                    assert response.body == "Not found" : "Expected 'Not found' in the response body, but got ${response.body}"
                } else {
                    assert false : "Unexpected status code: ${response.statusCode} and body: ${response.body}"
                }
            }
        }
    }

    private WebClient createWebClient(int port) {
        return WebClient.builder()
                .baseUrl("http://localhost:$port")
                .build()
    }


}