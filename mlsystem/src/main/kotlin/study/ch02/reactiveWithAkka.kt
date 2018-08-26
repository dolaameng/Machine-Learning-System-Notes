package study.ch02

import akka.actor.*
import akka.japi.pf.ReceiveBuilder
import scala.Option
import scala.PartialFunction
import scala.concurrent.duration.Duration
import scala.runtime.BoxedUnit
import java.util.concurrent.ThreadLocalRandom

// How to use Akka to be reactive
// The scenario is to store votes into a unreliable database,
// and losing part of votes is not important, and kinda expected.
// However, the system being RESPONSIVE is the first priority.

// Simulate a lousy database that does the job only half time
// and throws an Exception the other half
class DBException(message:String): Throwable(message = message)

class LousyDatabase (private val url: String) {
    val votes: MutableMap<String, Any> = HashMap()

    fun insert(updates: Map<String, Any>) {
        val randy = ThreadLocalRandom.current()
        if (randy.nextBoolean()) {
            throw DBException("LousyDatabase IO Error")
        } else {
            updates.forEach { k, v ->
                votes[k] = v
            }
        }
    }
}
// Votes that will be stored in the database
data class Vote(val timestamp: Long, val voterId: Long, val howler: String)

// Akka Actors - see howToUseAkka.kt for some examples
// 1. writer actor - responds to write data to the database
class DBWriter(val db: LousyDatabase): AbstractLoggingActor() {
    override fun createReceive(): Receive = ReceiveBuilder().match(Vote::class.java) {vote ->
        if (vote != null) {
            val record = mapOf(
                    "timestamp" to vote.timestamp,
                    "voteId" to vote.voterId,
                    "howler" to vote.howler
            )
            db.insert(record)
        }

    }.build()

    // log the restart
    override fun preRestart(reason: Throwable?, message: Option<Any>?) {
        super.preRestart(reason, message)
        log().info("DBWriter actor is restarting...")
    }
}
// 2. supervisor actor - it creates the writer actor and supervise it
class DBSupervisor(val db: LousyDatabase) : AbstractLoggingActor() {
    override fun preStart() {
        super.preStart()
        // create the dbwriter actor to supervise
        context.actorOf(Props.create(DBWriter::class.java, db), "dbwriter")
    }

    override fun supervisorStrategy(): SupervisorStrategy = OneForOneStrategy(-1, Duration.Inf()) {
        when (it) {
            is DBException -> SupervisorStrategy.restart()
            else -> SupervisorStrategy.stop()
        }
    }

    override fun createReceive(): Receive = ReceiveBuilder().match(Vote::class.java) { message ->
        context.children.forEach {it.tell(message, self())}
    }.build()
}

fun main(args: Array<String>) {
    val db = LousyDatabase("http://votedb/lousy")

    val actorSystem = ActorSystem.create("lousydb")
    val voteSystemWithSupervision = actorSystem.actorOf(Props.create(DBSupervisor::class.java, db), "votesystem")
    actorSystem.log().info("Vote System with supervision started...")
    actorSystem.log().info("Now send some votes, watch how the workers are restarting automatically and keep being responsive")

    val votes = listOf<Vote>(
            Vote(System.currentTimeMillis(), 1, "Panda"),
            Vote(System.currentTimeMillis(), 2, "Panda"),
            Vote(System.currentTimeMillis(), 3, "Panda"),
            Vote(System.currentTimeMillis(), 4, "Mikey")
    )
    votes.forEach {
        voteSystemWithSupervision.tell(it, ActorRef.noSender())
    }

    // after a while, check the db
    Thread.sleep(2000)
    println("Check what is written to the db now")
    db.votes.forEach {println(it)}
}