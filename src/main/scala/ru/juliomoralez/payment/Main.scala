package ru.juliomoralez.payment

import akka.actor.{ActorRef, ActorSystem, Props}
import ru.juliomoralez.payment.actors.{LogPayment, PaymentsReader, Start}
import ru.juliomoralez.payment.config.{PaymentConfig, UserConfig}

object Main extends App {

  // проверяем файлы конфигов на старте. В случае ошибки сразу падаем и не продолжаем программу
  PaymentConfig
  UserConfig

  implicit val system: ActorSystem = ActorSystem("system")

  val logPayment: ActorRef = system.actorOf(Props[LogPayment](), "logPayment")

  val paymentsReader: ActorRef = system.actorOf(Props(classOf[PaymentsReader], system, logPayment), "paymentsReader")
  paymentsReader ! Start

  Thread.sleep(20000)
  system.terminate()
}
