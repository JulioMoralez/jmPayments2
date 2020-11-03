package ru.juliomoralez.payment

import java.io.File

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.event.LoggingAdapter
import com.typesafe.config.ConfigFactory
import ru.juliomoralez.payment.actors.{LogPayment, PaymentsReader, Start}
import ru.juliomoralez.payment.config.{PaymentConfig, ProgramConfig, UsersConfig}
import ru.juliomoralez.payment.util.LoggerFactory

import scala.util.Try

object Main extends LoggerFactory{

  implicit val system: ActorSystem = ActorSystem("system")
  lazy private val log: LoggingAdapter = newLogger(system)
  val usersFileName: String = "src/main/resources/users.conf"

  def main(args: Array[String]): Unit = {
    safeReadConfig().fold(terminateProgram, starting)
    Thread.sleep(20000)
    system.terminate()
  }

  private def safeReadConfig(): Try[ProgramConfig] = {
    Try{
      val paymentConfig: PaymentConfig = PaymentConfig(ConfigFactory.load())
      val usersConfig = UsersConfig(ConfigFactory.parseFile(new File(usersFileName)))
      ProgramConfig(paymentConfig, usersConfig)
    }
  }

  private def starting(programConfig: ProgramConfig): Unit = {
    val logPayment: ActorRef = system.actorOf(Props(classOf[LogPayment], log, programConfig), "logPayment")
    val paymentsReader: ActorRef = system.actorOf(Props(classOf[PaymentsReader], system, log, logPayment, programConfig), "paymentsReader")
    paymentsReader ! Start
  }


  private def terminateProgram(ex: Throwable): Unit = {
    log.error("Program error " + ex)
    log.info("Terminating the actor system")
    system.terminate()
  }
}
