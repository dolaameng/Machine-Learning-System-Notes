package study.ch02

import kotlinx.coroutines.experimental.*
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.system.measureTimeMillis

// Some code demonstrating how to be reactive to uncertainty

// Simulate a voting system with different scenarios
object VoteSystem {
    private val totalVotes = mapOf(
            "Mikey" to 52,
            "nom nom" to 105
    )

    ////////////////// version 1: deal with non-existing keys /////////////////
    fun getVoteWithDefault(howler: String): Int = totalVotes.getOrDefault(howler, 0)

    ////////////////// version 2: deal with voting with a time delay /////////////////
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

    ////////////////// version 3: go further with the balance between latency and accuracy /////////////////
    // In most of machine learning system, getting a single accurate example of data is usually not that important.
    // We can sacrifice some accuracy for low latency. The benefit is that now we have some good estimate of the
    // system latency, which doesn't depend on outliers.
    // My implementation here uses exceptions and it is a little tedious and ugly. Any better way?
    fun getVoteWithTimeout(howlers: List<String>, timedOutInMillisecs: Int, defaultVote: Int): List<DelayedVoteResult> {
        val voteJobs = howlers.map { it to getVoteWithDelay(it) }
        return try {
            runBlocking {
                withTimeout(timedOutInMillisecs.toLong(), TimeUnit.MILLISECONDS) {
                    // don't use a blind delay like delay(timedOutInMillisecs) because the jobs may all finish before that
                    voteJobs.map { (howler, job) ->
                        job.await()
                    }
                }
            }
        } catch (e: TimeoutCancellationException) {
            voteJobs.map { (howler, job) ->
                if (job.isCompleted)
                    job.getCompleted()
                else
                    DelayedVoteResult(howler, defaultVote, timedOutInMillisecs)
            }
        }
    }
}

// test functions
fun testVoteWithDefault() {
    println("======Test Vote with Default Value:======")
    val voteForMikey = VoteSystem.getVoteWithDefault("Mikey")
    val voteForPanda = VoteSystem.getVoteWithDefault("Panda")
    println("Vote for Mikey $voteForMikey")
    println("Vote for Panda $voteForPanda")
}

fun testVoteWithDelay() {
    println("======Test Vote with Delayed Value:======")

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

fun testVoteWithTimeout() {

    val timedOutInMillisecs = 500
    val defaultVote = 42
    println("======Test Vote with a timeout $timedOutInMillisecs millisecs and default vote $defaultVote======")
    val howlers = listOf("Mikey", "nom nom", "Panda", "Mr X")
    val queryTime = measureTimeMillis {
        val votes = VoteSystem.getVoteWithTimeout(howlers, timedOutInMillisecs, defaultVote)
        votes.forEach{println(it)}
    }
    println("Finish timeout vote in $queryTime millisecs")
    println("Note that now the finishing time is upper bounded by timeout, any latency longer than it will return default vote")
}

fun main(args: Array<String>) {
    testVoteWithDefault()
    println()
    testVoteWithDelay()
    println()
    testVoteWithTimeout()
}