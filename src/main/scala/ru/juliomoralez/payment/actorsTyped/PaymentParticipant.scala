package ru.juliomoralez.payment.actorsTyped

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior, Recovery}
import ru.juliomoralez.payment.actorsTyped.LogPayment.{InfoMessage, JournalOperation}

object PaymentParticipant extends Serializable {

  sealed trait PaymentSign
  final case object Plus extends PaymentSign
  final case object Minus extends PaymentSign

  sealed trait PaymentOperation
  final case class Payment(sign: PaymentSign, value: Long, participant: ActorRef[PaymentParticipant.PaymentOperation]) extends PaymentOperation
  final case class StopPayment(p: Payment) extends PaymentOperation
  final case object PrintValue extends PaymentOperation

  sealed trait Event
  final case class Changed(balance: Long) extends Event

  final case class State(balance: Long)

  def apply(name: String, startValue: Long, logPayment: ActorRef[JournalOperation]): Behavior[PaymentOperation] =
    Behaviors.setup { context =>

      def paymentMinus(p: Payment, state: State): Long = {
        if ((state.balance - p.value) >= 0) {
          val newBalance = state.balance - p.value
          logPayment ! InfoMessage(
            s"Payment successful `-` ${context.self.path.name} -> ${p.participant.path.name} : ${p.value}. Balance ${context.self.path.name} = $newBalance")
          p.participant ! Payment(Plus, p.value, context.self)
          newBalance
        } else {
          context.self ! StopPayment(p)
          state.balance
        }
      }

      def paymentPlus(p: Payment, state: State): Long = {
        val newBalance = state.balance + p.value
        logPayment ! InfoMessage(
          s"Payment successful `+` ${p.participant.path.name} -> ${context.self.path.name} : ${p.value}. Balance ${context.self.path.name} = $newBalance")
        newBalance
      }

      def printValue(state: State): Unit = {
        context.log.info(s"${context.self.path.name} balance=${state.balance}")
      }

      def stopPayment(p: Payment, state: State): Unit = {
        logPayment ! InfoMessage(
          s"Canceling a payment ${context.self.path.name} -> ${p.participant.path.name} : ${p.value} . Balance ${context.self.path.name} = ${state.balance}")
      }

      val commandHandler: (State, PaymentOperation) => Effect[Event, State] = { (state, command) =>
        command match {
          case p @ Payment(Minus, _, _) =>
            Effect.persist(Changed(paymentMinus(p, state)))
          case p @ Payment(Plus, _, _) =>
            Effect.persist(Changed(paymentPlus(p, state)))
          case PrintValue =>
            printValue(state)
            Effect.none
          case StopPayment(p) =>
            stopPayment(p, state)
            Effect.none
        }
      }

      val eventHandler: (State, Event) => State = { (_, event) =>
        event match {
          case Changed(newBalance) => State(newBalance)
        }
      }

      EventSourcedBehavior[PaymentOperation, Event, State](
        persistenceId = PersistenceId.ofUniqueId(name),
        emptyState = State(startValue),
        commandHandler = commandHandler,
        eventHandler = eventHandler)
          .withRecovery(Recovery.disabled)
    }
}
