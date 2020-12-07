package ru.juliomoralez.payment.actorsTyped

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import ru.juliomoralez.payment.config.PaymentConfig

object LogPayment extends Serializable {

  sealed trait JournalOperation
  final case class InfoMessage(message: String) extends JournalOperation
  final case class ErrorMessage(message: String) extends JournalOperation

  def apply(paymentConfig: PaymentConfig): Behavior[JournalOperation] = {

    Behaviors.receive { (context, message) =>
      message match {
        case InfoMessage(text) => addMessage(s"[INFO] $text", context)
        case ErrorMessage(text) => addMessage(s"[ERROR] $text", context)
      }
    }
  }

  def addMessage(text: String, context:ActorContext[JournalOperation]): Behavior[JournalOperation] = {
    context.log.info(text)
    Behaviors.same
  }
}
