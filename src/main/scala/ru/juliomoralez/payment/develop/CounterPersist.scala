package ru.juliomoralez.payment.develop

import akka.actor.{ActorLogging, ActorRef, ActorSystem, Props}
import akka.persistence.{PersistentActor, SnapshotOffer}
import ru.juliomoralez.payment.develop.Counter.{Cmd, Dec, Evt, Inc, State}

object Counter {
  sealed trait Operation {
    val count: Int
  }

  case class Inc(override val count: Int) extends Operation
  case class Dec(override val count: Int) extends Operation

  case class Cmd(op: Operation)
  case class Evt(op: Operation)

  case class State(count: Int)
}

class Counter extends PersistentActor with ActorLogging {
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
      println("E " + evt)
      updateState(evt)
    case SnapshotOffer(_, snapshot: State) =>
      println("S " + snapshot)
      state = snapshot
  }

  override def receiveCommand: Receive = {
    case cmd @ Cmd(op) =>
      println(cmd)
      persist(Evt(op)) { evt => updateState(evt)}
    case "print" =>
      println("P " + state)
  }
}

object Main extends App {
  val system: ActorSystem = ActorSystem("system1")
  val c: ActorRef = system.actorOf(Props[Counter])

  Thread.sleep(1000)
  system.terminate()
}
