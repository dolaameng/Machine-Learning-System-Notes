package study.ch02

import akka.actor.*
import akka.japi.pf.ReceiveBuilder
import kotlinx.coroutines.experimental.channels.actor
import scala.Option
import scala.concurrent.duration.Duration

// Some demonstration of how to start Akka with Kotlin
// It follows hello world example at
// https://developer.lightbend.com/guides/akka-quickstart-java/define-actors.html
// https://medium.com/blockchain-engineering/kotlin-akka-part-1-hello-kotlin-a0c0de7d7407

fun part1_actors_can_receive_and_respond() {
    println("======== Part 1 Actors can receive and respond ========")

    // A Message to an akka actor can be anything
    data class ActorMessage(val secret: String)

    // An actor class derived from a template
    // a. AbstractLoggingActor enables the access to log() method for actor so it is able to communicate with stdio
    // b. Most important method is `createReceive`, which create a `Receive` behavior for the agent that responds to message
    // c. Use `ReceiveBuilder` to create the receive behavior object for the agent, first it matches a type of message,
    //    followed by a lambda specifying behavior, then it can be built.
        class HelloActor: AbstractLoggingActor() {
            override fun createReceive(): Receive =
                    ReceiveBuilder().match(ActorMessage::class.java) {
                        log().info(it.secret)
                    }.build()
        }

    // An actor system is the world that actors live in
    val actorSystem = ActorSystem.create("part1")

    // An actor prop is the meta data for an actor, it basically "translate" an actor class into a serialized actor
    // template so they can be distributed and populated later
    val actorProp = Props.create(HelloActor::class.java)

    // An actor reference is the container of an actor. Intuitively it is like
    // the house with a MAILBOX that the agent will live in.
    // You can explicitly specify the agent's name like here, or an internal name will be created by system like /path/to/@a
    val actorRef = actorSystem.actorOf(actorProp, "Agent-Happy")

    // The actor system has the logging ability
    actorSystem.log().info("From system: Sending a secrete message to hello actor")

    // Finally, we send a message to the hello agent's mailbox, and it will respond accordingly.
    // Under the hood a `dispatcher` will deliver the message to the agent's mailbox, the agent is uniquely
    // identified by its name, i.e., "Agent-Happy" in this example.
    // The agent is acting in a different "thread" than dispatcher.
    actorRef.tell(ActorMessage("From helloActor: I was told 'hello akka from kotlin'"), ActorRef.noSender())

}

fun part2_actors_are_supervised() {
    // actors are supervised by the actor system by default.
    // but it is more common for parent actors to supervise their children.
    // Supervisions are basically mapping from Exceptions to Directives.
    println("======== Part 2 Actors are supervised ========")

    // A simple worker actor that will
    // - print the message received except it is "QUIT"
    // - throw quit exception when received "QUIT" message
    // - print when it restarts
    class WorkerActor: AbstractLoggingActor() {
        override fun createReceive(): Receive = ReceiveBuilder().match(String::class.java) { message ->
            when (message) {
                "QUIT" -> throw Exception("I QUIT")
                else -> log().info(message)
            }
        }.build()

        override fun preRestart(reason: Throwable?, message: Option<Any>?) {
            super.preRestart(reason, message)
            log().info("I am restarting!!")
        }
    }

    // A supervisor actor is similar to a normal actor, with the differences:
    // - it spawns child actors, usually in `preStart`. this can be done by using an actor's context
    // - it has a supervisorStategy, mapping children's exceptions to directives. There are usually two kinds
    //   OneForOneStrategy or AllForOneStrategy. A strategy can be specified with a max # of tries or a window.
    // - it's createReceive usually involves forwarding the messages to its children.
    class SupervisorActor: AbstractLoggingActor() {
        override fun createReceive(): Receive = ReceiveBuilder().match(String::class.java) {
            context.children.forEach {child -> child.tell(it, self())}
        }.build()
        override fun supervisorStrategy(): SupervisorStrategy = AllForOneStrategy(-1, Duration.Inf()) {
            when (it) {
                is ActorInitializationException -> SupervisorStrategy.stop()
                is ActorKilledException -> SupervisorStrategy.stop()
                is DeathPactException -> SupervisorStrategy.stop()
                else -> SupervisorStrategy.restart()
            }
        }
        override fun preStart() {
            super.preStart()
            context.actorOf(Props.create(WorkerActor::class.java), "worker1")
            context.actorOf(Props.create(WorkerActor::class.java), "worker2")
            context.actorOf(Props.create(WorkerActor::class.java), "worker3")
        }
    }

    val actorSystem = ActorSystem.create("part2")
    val supervisorActorRef = actorSystem.actorOf(Props.create(SupervisorActor::class.java), "supervisor")
    actorSystem.log().info("Sending hello to supervisor actor, expect workers to receive and print it")
    supervisorActorRef.tell("Hello Actors", ActorRef.noSender())
    actorSystem.log().info("Sending QUIT to worker 1, expect the worker1 to quit." +
            "This will result in a restart of all worker agents based on supervior's strategy.")
    actorSystem.actorSelection("akka://part2/user/supervisor/worker2")
            .tell("QUIT", ActorRef.noSender())
}



fun main(args: Array<String>) {
//    part1_actors_can_receive_and_respond()
    part2_actors_are_supervised()
}