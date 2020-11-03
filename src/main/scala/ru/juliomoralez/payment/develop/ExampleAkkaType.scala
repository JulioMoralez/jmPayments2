package ru.juliomoralez.payment.develop

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import ru.juliomoralez.payment.develop.PaymentParticipant.paymentParticipant


sealed trait PaymentSign
case object Plus extends PaymentSign
case object Minus extends PaymentSign

sealed trait PaymentOperation
case class Payment(sign: PaymentSign, value: Long, participant: ActorRef[PaymentOperation]) extends PaymentOperation
case object StopPayment extends PaymentOperation
case object PrintValue extends PaymentOperation

object PaymentParticipant {

  def paymentParticipant(value: Long = 0): Behavior[PaymentOperation] =
    Behaviors.receive { (context, message) =>

      message match {
        case Payment(Minus, v, _) =>
          if ((value - v) >= 0) {
            println(Minus)
            paymentParticipant(value - v)
          } else {
            println("отмена1")
            StopPayment
            Behaviors.same
          }
        case Payment(Plus, v, _) =>
          paymentParticipant(value + v)
        case StopPayment =>
          println("отмена")
          Behaviors.same
        case PrintValue =>
          println(value)
          Behaviors.same
        case _ =>
          Behaviors.same
      }
    }
}

object Main2 extends App {
  val p: ActorSystem[PaymentOperation] = ActorSystem(paymentParticipant(100), "paymentParticipant")
  p ! Payment(Minus, 10, p)
  p ! Payment(Minus, 100, p)
  p ! Payment(Plus, 5, p)
  p ! PrintValue

  Thread.sleep(1000)
  p.terminate()
}
