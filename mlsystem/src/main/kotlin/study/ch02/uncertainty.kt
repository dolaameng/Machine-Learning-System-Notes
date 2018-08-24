package study.ch02

import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.runBlocking
import java.util.*

// Some code demonstrating how to be reactive to uncertainty

// Simulate a voting system with different scenarios

object VoteSystem {
    private val totalVotes = mapOf(
            "Mikey" to 52,
            "nom nom" to 105
    )

    ////////////////// version 1: deal with non-existing keys
    fun getVoteWithDefault(howler: String): Int = totalVotes.getOrDefault(howler, 0)

    ////////////////// version 2: deal with voting with a time delay
    // simulate voting with random delay
    data class DelayedVoteResult(val howler: String, val vote: Int, val delay: Int)
    private suspend fun remoteVote(howler: String): DelayedVoteResult {
        val delayTime = Random().nextInt(1000)
        delay(delayTime.toLong())
        return DelayedVoteResult(howler, getVoteWithDefault(howler), delayTime)
    }
    // implemented by kotlin coroutines (see howToUseCoroutine.kt for details), makes an async call and
    // return a future object
    fun getVoteWithDelay(howler: String): Deferred<DelayedVoteResult> {
        return async {
            remoteVote(howler)
        }
    }
}

fun testVoteWithDefault() {
    println("Test Vote with Default Value:")
    val voteForMikey = VoteSystem.getVoteWithDefault("Mikey")
    val voteForPanda = VoteSystem.getVoteWithDefault("Panda")
    println("Vote for Mikey $voteForMikey")
    println("Vote for Panda $voteForPanda")
}

fun testVoteWithDelay() {
    println("Test Vote with Default Value:")

    val howlers = listOf("Mikey", "nom nom", "Panda", "Mr X")

    val startTime = System.currentTimeMillis()
    // start query in parallel and immediately return deferred result
    val votes = howlers.map {
        VoteSystem.getVoteWithDelay(it)
    }
    val queryStartedTime = System.currentTimeMillis()
    // block wait to collect the result, the waiting time depends on the longest-running element
    runBlocking {
        votes.map {it.await()}.forEach {println(it)}
    }
    val queryFinishedTime = System.currentTimeMillis()
    println("Start queries in deferred parallel in ${queryStartedTime-startTime} millis " +
            "and finished query in another ${queryFinishedTime-queryStartedTime} millis")
    println("The start query time should be constantly small " +
            "and the finish query time should be close to the longest vote time")


}

fun main(args: Array<String>) {
    testVoteWithDefault()
    println()
    testVoteWithDelay()
}