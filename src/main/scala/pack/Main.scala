package pack

import akka.actor.{ActorRef, ActorSystem, Props}

object Main extends App {

  implicit val system: ActorSystem = ActorSystem("system")

  val logPayment: ActorRef = system.actorOf(Props[LogPayment](), "logPayment")

  val paymentsReader: ActorRef = system.actorOf(Props(classOf[PaymentsReader], system, logPayment), "paymentsReader")
  paymentsReader ! Start

  Thread.sleep(20000)
  system.terminate()
}
