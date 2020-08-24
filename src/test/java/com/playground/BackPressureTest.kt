package com.playground

import io.kotlintest.shouldBe
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Duration

class BackPressureTest {
    @Test
    fun `bufferTimeoutWithBackpressure works as intended`() {
        Flux.fromIterable(1..1000)
            .delayElements(Duration.ofMillis(1))
            .bufferTimeoutWithBackpressure(5, Duration.ofMillis(2)) {
                concatMap {
                    Mono.delay(Duration.ofMillis(20))
                        .thenReturn(it)
                }
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
    fun `bufferTimeout does not work`() {
        Flux.fromIterable(1..1000)
            .delayElements(Duration.ofMillis(1))
            .doOnRequest { req -> println("Before buffer timeout: $req") }
            .bufferTimeout(5, Duration.ofMillis(2))
            .doOnRequest { req -> println("After buffer timeout: $req") }
            .concatMap {
                Mono.delay(Duration.ofMillis(1))
                    .doOnRequest { req -> println("buffered: $it. Req: $req") }
                    .thenReturn(it)
            }
            .doOnRequest { req -> println("Before concatMap : $req") }
            .blockLast()
    }
}