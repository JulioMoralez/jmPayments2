package ru.juliomoralez.payment.develop


import akka.actor.{ActorLogging, ActorRef, ActorSystem, Props}
import akka.persistence.{PersistentActor, SnapshotOffer}
import ru.juliomoralez.payment.develop.Counter1.{Cmd, Dec, Evt, Inc, State}

object Counter1 extends Serializable{
  sealed trait Operation extends Serializable{
    val count: Int
  }

  case class Inc(override val count: Int) extends Operation
  case class Dec(override val count: Int) extends Operation

  case class Cmd(op: Operation) extends Serializable
  case class Evt(op: Operation) extends Serializable

  case class State(count: Int) extends Serializable
}

class Counter extends PersistentActor with ActorLogging with Serializable{
  override def persistenceId: String = "example"

  var state: State = State(count = 0)

  def updateState(evt: Evt): Unit = {
    evt match {
      case Evt(Inc(count)) =>
        state = State(count = state.count + count)
      case Evt(Dec(count)) =>
        state = State(count = state.count - count)
    }
  }

  override def receiveRecover: Receive = {
    case evt: Evt =>
      updateState(evt)
    case SnapshotOffer(_, snapshot: State) =>
      println("S " + snapshot)
      state = snapshot
  }

  override def receiveCommand: Receive = {
    case cmd @ Cmd(op) =>
      persist(Evt(op)) { evt => updateState(evt)}
    case "print" =>
      println("P " + state)
  }
}

object Main extends App {
  val system: ActorSystem = ActorSystem("system1")
  val counter: ActorRef = system.actorOf(Props[Counter])

  counter ! Cmd(Inc(3))
  counter ! Cmd(Inc(5))
  counter ! Cmd(Dec(2))
  counter ! "print"

  Thread.sleep(1000)
  system.terminate()
}
