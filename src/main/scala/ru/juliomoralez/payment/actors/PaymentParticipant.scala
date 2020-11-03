package ru.juliomoralez.payment.actors

import akka.actor.{Actor, ActorRef}
import akka.pattern.ask
import akka.util.Timeout
import ru.juliomoralez.payment.config.UsersConfig

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}

sealed class PaymentSign
case object Plus extends PaymentSign
case object Minus extends PaymentSign

case class Payment(sign: PaymentSign, value: Long, participant: ActorRef)
case object StopPayment

class PaymentParticipant(logPayment: ActorRef, usersConfig: UsersConfig) extends Actor{

  val DEFAULT_VALUE: Long = 100 // начальное значение баланса, если участник не указан в application.conf

  implicit val timeout: Timeout = 5.seconds
  var value: Long = 0

  override def preStart(): Unit = {
    value = if (usersConfig.usersStartValue.contains(self.path.name)) {
      usersConfig.usersStartValue(self.path.name)
    } else {
      DEFAULT_VALUE
    }
  }

  def receive: Receive = {
    case Payment(paymentSign, v, participant) =>
      paymentSign match {
        case Plus =>
          value += v
          logPayment ! AddJournalMessage(s"Операция '+' ${participant.path.name} -> ${self.path.name} : $v успешна. Баланс ${self.path.name} = $value")
        case Minus =>
          if ((value - v) >= 0) {
          value -= v
          val future: Future[Any] = participant.ask(Payment(Plus, v, self))
          Await.result(future, timeout.duration)
          logPayment ! AddJournalMessage(s"Операция '-' ${self.path.name} -> ${participant.path.name} : $v успешна. Баланс ${self.path.name} = $value")
        } else {
          self ! StopPayment
        }
      }
      sender ! false
    case StopPayment =>
      logPayment ! AddJournalMessage(  s"Отмена. Недостаточно средств. Баланс ${self.path.name} = $value")
  }
}
