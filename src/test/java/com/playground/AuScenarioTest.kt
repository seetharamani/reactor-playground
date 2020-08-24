package com.playground

import io.kotlintest.shouldBe
import org.junit.jupiter.api.Test
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Duration

class AuScenarioTest {

    @Test
    fun `bufferTimeoutWithBackpressure fails as intended`() {
        Flux.fromIterable(1..1000)
            .delayElements(Duration.ofMillis(1))
            .bufferWithBackpressureFail(5, Duration.ofMillis(2))
            .concatMap {
                Mono.delay(Duration.ofMillis(20))
                    .thenReturn(it)
            }
            .concatMapIterable { it }
            .reduce(0) { lastSeen, new ->
                new shouldBe lastSeen + 1
                // assertThat(new).isEqualTo(lastSeen + 1)
                if (new % 100 == 0) {
                    print { "Got $new" }
                }
                new
            }
            .block()
    }

    @Test
    fun `no buffer or whatsoever`() {
        Flux.fromIterable(1..1000)
            //.delayElements(Duration.ofMillis(1))
            .doOnRequest { println("Requested before flatmap : $it") }
            .flatMap {
                Mono.delay(Duration.ofMillis(10000))
                    .thenReturn(it)
            }
            .doOnRequest { println("Requested after flatmap : $it") }
            .doOnError { println("Error in the flux : $it") }
            .reduce(0) { _, new ->
                if (new % 100 == 0) {
                    print { "Got $new" }
                }
                new
            }
            .block()
    }
}