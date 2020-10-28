package pack

import akka.actor.{Actor, ActorRef}
import akka.pattern.ask
import akka.util.Timeout
import pack.MyConfiguration.usersStartValue

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}

case class PaymentSign(sign: String) {
  override def toString: String = sign
}
case class Payment(sign: PaymentSign, value: Long, participant: ActorRef)
case object StopPayment

class PaymentParticipant(logPayment: ActorRef) extends Actor{

  val DEFAULT_VALUE: Long = 100 // начальное значение баланса, если участник не указан в application.conf

  implicit val timeout: Timeout = 5.seconds
  private var value: Long = 0

  override def preStart(): Unit = {
    value = if (usersStartValue.contains(self.path.name)) usersStartValue(self.path.name) else DEFAULT_VALUE
  }

  def receive: Receive = {
    case Payment(paymentSign, v, participant) =>
      if (paymentSign.sign == "+") {
        value += v
        logPayment ! AddJournalMessage(s"Операция '+' ${participant.path.name} -> ${self.path.name} : $v успешна. Баланс ${self.path.name} = $value")
      }
      if (paymentSign.sign == "-") {
        if ((value - v) >= 0) {
          value -= v
          val future: Future[Any] = participant.ask(Payment(PaymentSign("+"), v, self))
          Await.result(future, timeout.duration)
          logPayment ! AddJournalMessage(s"Операция '-' ${self.path.name} -> ${participant.path.name} : $v успешна. Баланс ${self.path.name} = $value")
        } else {
          self ! StopPayment
        }
      }
      sender ! ()
    case StopPayment =>
      logPayment ! AddJournalMessage(  s"Отмена. Недостаточно средств. Баланс ${self.path.name} = $value")

  }
}
