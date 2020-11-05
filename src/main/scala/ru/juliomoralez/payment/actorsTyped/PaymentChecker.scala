package ru.juliomoralez.payment.actorsTyped

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import ru.juliomoralez.payment.config.ProgramConfig

import scala.collection.mutable
import scala.util.matching.Regex

object PaymentChecker {

  val users: mutable.Map[String, ActorRef[PaymentOperation]] = mutable.Map()

  def apply(programConfig: ProgramConfig, logPayment: ActorRef[JournalOperation]): Behavior[String] = {
    Behaviors.receive { (context, message) =>
      val paymentRegex: Regex = programConfig.paymentConfig.paymentRegex.r()

      def checkTransaction(transaction: String): Behavior[String] = {
        // проверка входной строки на <NAME1> -> <NAME2>: <VALUE>
        transaction match {
          case paymentRegex(from, _, to, _, value) =>
            logPayment ! AddJournalMessage(from + " " + to + " " + value)

            def createActor(name: String): Unit = {
              if (!users.contains(name)) {
                val startValue: Long = if (programConfig.usersConfig.usersStartValue.contains(name)) {
                    programConfig.usersConfig.usersStartValue(name)
                  } else {
                    programConfig.usersConfig.defaultUserValue
                  }
                val paymentParticipant: ActorRef[PaymentOperation] = context.spawn(PaymentParticipant(startValue, logPayment), name)
                users += (name -> paymentParticipant)
              }
            }

            createActor(from)
            createActor(to)
            users(from) ! Payment(Minus, value.toLong, users(to))
          case _ =>
            logPayment ! ErrorMessage("error " + transaction)
        }
        Behaviors.same
      }

      message match {
        case transaction: String => checkTransaction(transaction)
      }
    }
  }
}
