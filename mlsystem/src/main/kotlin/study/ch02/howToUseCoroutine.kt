package study.ch02

import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.runBlocking
import kotlin.system.measureTimeMillis

fun rightWay() {
    val xs = (1..10).map {
        async {
            delay(1000)
            it
        }
    }
    measureTimeMillis {
        runBlocking {
            xs.sumBy { it.await() }.let {println("sum is $it")}
        }
    }.let {
        println("run time $it")
    }
}

fun wrongWay() {

    measureTimeMillis {
        runBlocking {
            val xs = (1..10).map {
                async {
                    delay(1000)
                    it
                }.await()
            }
            xs.sumBy { it }.let {println("sum is $it")}
        }
    }.let {
        println("run time $it")
    }
}

fun main(args: Array<String>) {
    println("Right way should start coroutines in parallel without suspending the main thread, and only" +
            "blockRunning when collecting the result")
    rightWay()
    println()
    println("Wrong way will start and wait on the result at the same time, so it is like not using parallel at all.")
    wrongWay()
}