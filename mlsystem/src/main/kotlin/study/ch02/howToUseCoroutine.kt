package study.ch02

import kotlinx.coroutines.experimental.*
import java.util.concurrent.TimeUnit
import kotlin.system.measureTimeMillis

// Demonstrate some traps when I use Kotlin coroutines as a beginner.

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