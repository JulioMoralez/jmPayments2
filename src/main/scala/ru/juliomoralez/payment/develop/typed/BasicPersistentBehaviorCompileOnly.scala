package ru.juliomoralez.payment.develop.typed

import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.persistence.typed.scaladsl.EventSourcedBehavior
import akka.persistence.typed.PersistenceId
import ru.juliomoralez.payment.actorsTyped.PaymentSystem
import ru.juliomoralez.payment.develop.typed.BasicPersistentBehaviorCompileOnly.FirstExample.{Add, Clear}
import ru.juliomoralez.payment.develop.typed.BasicPersistentBehaviorCompileOnly.MyPersistentBehavior.C

//#behavior
//#structure
import akka.persistence.typed.RecoveryCompleted
import akka.persistence.typed.SnapshotFailed

// unused variables in pattern match are useful in the docs
object BasicPersistentBehaviorCompileOnly extends App {

  import akka.persistence.typed.scaladsl.RetentionCriteria

  object FirstExample {

    //#command
    sealed trait Command

    final case class Add(data: String) extends Command

    case object Clear extends Command

    sealed trait Event

    final case class Added(data: String) extends Event

    case object Cleared extends Event

    //#command

    //#state
    final case class State(history: List[String] = Nil)

    //#state

    //#command-handler

    import akka.persistence.typed.scaladsl.Effect

    val commandHandler: (State, Command) => Effect[Event, State] = { (state, command) =>
      println(state + " " + command)
      command match {
        case Add(data) => Effect.persist(Added(data))
        case Clear => Effect.persist(Cleared)
      }
    }
    //#command-handler

    //#effects
    def onCommand(subscriber: ActorRef[State], state: State, command: Command): Effect[Event, State] = {
      command match {
        case Add(data) =>
          Effect.persist(Added(data)).thenRun(newState => subscriber ! newState)
        case Clear =>
          Effect.persist(Cleared).thenRun((newState: State) => subscriber ! newState).thenStop()
      }
    }

    //#effects

    //#event-handler
    val eventHandler: (State, Event) => State = { (state, event) =>
      event match {
        case Added(data) => state.copy((data :: state.history).take(5))
        case Cleared => State(Nil)
      }
    }
    //#event-handler

    //#behavior
    def apply(id: String): Behavior[Command] =
      EventSourcedBehavior[Command, Event, State](
        persistenceId = PersistenceId.ofUniqueId(id),
        emptyState = State(Nil),
        commandHandler = commandHandler,
        eventHandler = eventHandler)

    //#behavior

  }

  //#structure
  object MyPersistentBehavior {

    sealed trait Command
    case class C(s: String) extends Command

    sealed trait Event

    final case class State()

    def apply(): Behavior[Command] =
      EventSourcedBehavior[Command, Event, State](
        persistenceId = PersistenceId.ofUniqueId("abc"),
        emptyState = State(),
        commandHandler = (state, cmd) => {
          println(state + " " + cmd)
          throw new NotImplementedError("TODO: process the command & return an Effect")
        },
        eventHandler = (state, evt) => throw new NotImplementedError("TODO: process the event return the next state"))
  }

  val system = ActorSystem(FirstExample("123"), "system")
  system ! Add("qwert")
//  system ! Clear
  Thread.sleep(1000)
  system.terminate()

}