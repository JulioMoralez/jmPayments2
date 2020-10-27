package pack

import akka.actor.{ActorRef, ActorSystem, Props}

import scala.collection.mutable

case object Start
case class CheckPayment(payment: String)
case class ErrorMessage(message: String)
case class AddJournalMessage(message: String)
case class PaymentSign(sign: String) {
  override def toString: String = sign
}
case class Payment(sign: PaymentSign, value: Long, participant: ActorRef)
case object StopPayment


object Main extends App {

  val persons: mutable.Map[String, ActorRef] = mutable.Map()

  implicit val system: ActorSystem = ActorSystem("system")

  val logPayment: ActorRef = system.actorOf(Props[LogPayment](), "logPayment")
  val paymentChecker: ActorRef = system.actorOf(Props(classOf[PaymentChecker]), "paymentChecker")

  val paymentsReader: ActorRef = system.actorOf(Props(classOf[PaymentsReader], system), "paymentsReader")
  paymentsReader ! Start

  Thread.sleep(5000)
  system.terminate()



}
