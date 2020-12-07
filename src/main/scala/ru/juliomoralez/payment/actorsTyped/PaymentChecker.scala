package ru.juliomoralez.payment.actorsTyped

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import ru.juliomoralez.payment.actorsTyped.LogPayment.{ErrorMessage, InfoMessage, JournalOperation}
import ru.juliomoralez.payment.actorsTyped.PaymentParticipant.{Minus, Payment}
import ru.juliomoralez.payment.config.ProgramConfig

import scala.collection.mutable

object PaymentChecker extends Serializable {

  final case class CheckPayment(payment: String)

  val users: mutable.Map[String, ActorRef[PaymentParticipant.PaymentOperation]] = mutable.Map()

  def apply(implicit programConfig: ProgramConfig, logPayment: ActorRef[JournalOperation]): Behavior[CheckPayment] = {
    Behaviors.receive { (context, message) =>
      message match {
        case CheckPayment(payment) => checkPayment(payment, context)
      }
    }
  }

  def checkPayment(payment: String, context: ActorContext[CheckPayment])
                  (implicit programConfig: ProgramConfig, logPayment: ActorRef[JournalOperation]): Behavior[CheckPayment] = {
    val paymentRegex = programConfig.paymentConfig.paymentRegex.r()
    payment match {
      case paymentRegex(from, _, to, _, value) =>
        logPayment ! InfoMessage(from + " " + to + " " + value)
        checkNewUsers(Vector(from, to), context)
        users(from) ! Payment(Minus, value.toLong, users(to))
      case _ =>
        logPayment ! ErrorMessage("Bad transaction " + payment)
    }
    Behaviors.same
  }

  def checkNewUsers(names: Vector[String], context: ActorContext[CheckPayment])
                   (implicit programConfig: ProgramConfig, logPayment: ActorRef[JournalOperation]): Unit = {
    names.foreach(name =>
      if (!users.contains(name)) {
        val startValue = if (programConfig.usersConfig.usersStartBalance.contains(name)) {
          programConfig.usersConfig.usersStartBalance(name)
        } else {
          programConfig.usersConfig.defaultUserBalance
        }
        val paymentParticipant: ActorRef[PaymentParticipant.PaymentOperation] = context.spawn(PaymentParticipant(name, startValue, logPayment), name)
        users += (name -> paymentParticipant)
      }
    )
  }
}
