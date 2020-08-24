package com.playground

import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.util.concurrent.Queues
import reactor.util.context.Context
import java.time.Duration
import java.util.concurrent.Semaphore


fun <T> Flux<T>.bufferTimeoutWithBackpressure(
    maxSize: Int, maxTime: Duration, maxInFlightElements: Int = (maxSize * 4).coerceAtLeast(
        Queues.SMALL_BUFFER_SIZE
    ), remainingSteps: Flux<List<T>>.() -> Flux<List<T>>
): Flux<List<T>> =
    this
        .concatMap {
            Mono.subscriberContext()
                .map { context -> context.get("semaphore") as Semaphore }
                .doOnNext { semaphore -> if (!semaphore.tryAcquire()) throw RuntimeException() }
                .onErrorStop()
                .retryBackoff(Long.MAX_VALUE, Duration.ofMillis(1), Duration.ofMillis(100))
                .thenReturn(it)
        }
        .bufferTimeout(maxSize, maxTime)
        .onBackpressureBuffer(maxInFlightElements)
        .run(remainingSteps)
        .concatMap { list ->
            Mono.subscriberContext()
                .map { it.get("semaphore") as Semaphore }
                .doOnNext { semaphore -> semaphore.release(list.size) }
                .thenReturn(list)
        }
        .subscriberContext(Context.of("semaphore", Semaphore(maxInFlightElements)))
