package ru.juliomoralez.payment.develop

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import ru.juliomoralez.payment.actorsTyped.{AddJournalMessage, JournalOperation}

sealed trait PaymentSign
case object Plus extends PaymentSign
case object Minus extends PaymentSign

sealed trait PaymentOperation
case class Payment(sign: PaymentSign, value: Long, participant: ActorRef[PaymentOperation]) extends PaymentOperation
case object StopPayment extends PaymentOperation
case object PrintValue extends PaymentOperation

object PaymentParticipant {
  def apply(value: Long = 0, logPayment: ActorRef[JournalOperation]): Behavior[PaymentOperation] =
    Behaviors.receive { (context, message) =>

      def paymentMinus(p: Payment): Behavior[PaymentOperation] = {
          if ((value - p.value) >= 0) {
            val newValue = value - p.value
            logPayment ! AddJournalMessage(
              s"Операция '-' ${context.self.path.name} -> ${p.participant.path.name} : ${p.value} успешна. Баланс ${context.self.path.name} = $newValue")
            p.participant ! Payment(Plus, p.value, context.self)
            PaymentParticipant(newValue, logPayment)
          } else {
            context.self ! StopPayment
            Behaviors.same
          }
      }

      def paymentPlus(p: Payment): Behavior[PaymentOperation] = {
        val newValue = value + p.value
        logPayment ! AddJournalMessage(
          s"Операция '+' ${p.participant.path.name} -> ${context.self.path.name} : ${p.value} успешна. Баланс ${context.self.path.name} = $newValue")
        PaymentParticipant(newValue, logPayment)
      }

      def stopPayment: Behavior[PaymentOperation] = {
        logPayment ! AddJournalMessage(  s"Отмена. Недостаточно средств. Баланс ${context.self.path.name} = $value")
        Behaviors.same
      }

      message match {
        case p @ Payment(Minus, _, _) => paymentMinus(p)
        case p @ Payment(Plus, _, _) => paymentPlus(p)
        case StopPayment => stopPayment
        case _ => Behaviors.same
      }
    }
}
