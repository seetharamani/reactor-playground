package com.playground

import io.reactivex.Completable
import io.reactivex.Flowable
import reactor.core.publisher.Flux
import reactor.util.concurrent.Queues
import java.time.Duration

fun <T> Flux<T>.bufferWithBackpressureFail(
    maxSize: Int, maxTime: Duration, maxInFlightElements: Int = (maxSize * 4).coerceAtLeast(
        Queues.SMALL_BUFFER_SIZE
    )
): Flux<List<T>> =
    this
        .doOnRequest {
            println("Requesting data... $it")
        }
        .bufferTimeout(maxSize, maxTime)
        .onBackpressureBuffer(maxInFlightElements)
        .doOnError {
            println("Exception occurred : $it")
        }





private fun <T> org.reactivestreams.Publisher<T>.toFlowable(): Flowable<T> {
    return Flowable.fromPublisher(this)
}
