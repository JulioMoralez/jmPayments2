package ru.juliomoralez.payment.actors

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.pattern.ask
import akka.util.Timeout
import ru.juliomoralez.payment.config.ProgramConfig

import scala.collection.mutable
import scala.concurrent.{Await, Future}
import scala.concurrent.duration.DurationInt
import scala.util.matching.Regex

case class CheckTransaction(transaction: String)

class PaymentChecker(system: ActorSystem, logPayment: ActorRef, programConfig: ProgramConfig) extends Actor {

  val users: mutable.Map[String, ActorRef] = mutable.Map()
  val paymentRegex: Regex = programConfig.paymentConfig.paymentRegex.r()
  implicit val timeout: Timeout = 5.seconds

  def receive: Receive = {
    case CheckTransaction(transaction) =>
      // проверка входной строки на <NAME1> -> <NAME2>: <VALUE>
        transaction match {
          case paymentRegex(from, _, to, _, value) =>
            logPayment ! AddJournalMessage(transaction)

            def createActor(name: String): Unit = {
              if (!users.contains(name)) {
                val paymentParticipant: ActorRef = system.actorOf(Props(classOf[PaymentParticipant], logPayment, programConfig.usersConfig), name)
                users += (name -> paymentParticipant)
              }
            }
            createActor(from)
            createActor(to)
            val future: Future[Any] = users(from).ask(Payment(Minus, value.toLong, users(to)))
            Await.result(future, timeout.duration)
          case _ =>
            logPayment ! ErrorMessage(s"$transaction - ошибка в строке")
        }
      sender ! ()
      }
}
