package pack

import akka.actor.Actor
import pack.Main.logPayment

import scala.util.{Failure, Success}

class PaymentParticipant extends Actor{


  val DEFAULT_VALUE: Long = 100 // начальное значение баланса, если участник не указан в application.conf

  private var value: Long = 0

  override def preStart(): Unit = {
    MyConfiguration.get("persons." + self.path.name) match {
      case Success(v) => {
        try {
          value = v.toLong
        }
        catch {
          case e: Exception => value = DEFAULT_VALUE
        }
      }
      case Failure(_) => value = DEFAULT_VALUE
    }
  }

  def receive: Receive = {
    case Payment(paymentSign, v, participant) => {
      if (paymentSign.sign == "+") {
        value += v
        logPayment ! AddJournalMessage(s"Операция '+' ${participant.path.name} -> ${self.path.name} : ${v} успешна. Баланс ${self.path.name} = $value")
      }
      if (paymentSign.sign == "-") {
        if ((value - v) >= 0) {
          value -= v
          participant ! Payment(PaymentSign("+"), v, self);
          logPayment ! AddJournalMessage(s"Операция '-' ${self.path.name} -> ${participant.path.name} : $v успешна. Баланс ${self.path.name} = $value")
        } else {
          self ! StopPayment
        }
      }
    }
    case StopPayment => {
      logPayment ! AddJournalMessage(  s"Отмена. Недостаточно средств. Баланс ${self.path.name} = $value")
    }

  }
}
