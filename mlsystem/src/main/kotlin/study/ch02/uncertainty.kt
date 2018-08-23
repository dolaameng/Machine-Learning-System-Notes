package study.ch02

import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.runBlocking
import java.util.*
import kotlin.system.measureTimeMillis

// Some code demonstrating how to be reactive to uncertainty

// Simulate a voting system with different scenarios

object VoteSystem {
    val totalVotes = mapOf(
            "Mikey" to 52,
            "nom nom" to 105
    )




    // version 1: deal with non-existing keys
    fun getVoteWithDefault(howler: String): Int = totalVotes.getOrDefault(howler, 0)

    // version 2: deal with voting with a time delay


    // simulate voting with random delay
    private suspend fun remoteVote(howler: String): Int {
        delay(Random().nextInt(1000).toLong())
        return getVoteWithDefault(howler)
    }
    // implemented by kotlin coroutines, makes an async call and
    // return a future object
    fun getVoteWithDelay(howler: String): Deferred<Int> {
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

    val runTime = measureTimeMillis {
        // now running in parallel
        runBlocking {
            val votes = howlers.map {
                it to VoteSystem.getVoteWithDelay(it).await()
            }
            votes.forEach {
                println(it)
            }
        }
    }
    println("Finished in ${runTime/1000.0} seconds...")
}

fun main(args: Array<String>) {
    testVoteWithDefault()
    println()
    testVoteWithDelay()


}