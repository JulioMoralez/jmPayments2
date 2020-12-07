package ru.juliomoralez.payment

import java.io.File
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import com.typesafe.config.ConfigFactory
import ru.juliomoralez.payment.actorsTyped.LogPayment.JournalOperation
import ru.juliomoralez.payment.actorsTyped.PaymentsReader.{PaymentsReaderOperation, Start}
import ru.juliomoralez.payment.actorsTyped._
import ru.juliomoralez.payment.config.{PaymentConfig, ProgramConfig, UsersConfig}

import scala.util.Try

object PaymentSystem {

  def apply(): Behavior[Any] =
    Behaviors.setup { context =>
      context.log.info("Program start")
      safeReadConfig().fold(terminateProgram, starting)

      def safeReadConfig(): Try[ProgramConfig] = {
        Try{
          val paymentConfig = PaymentConfig(ConfigFactory.load())
          val usersConfig = UsersConfig(ConfigFactory.parseFile(new File(paymentConfig.usersConfigFilepath)))
          ProgramConfig(paymentConfig, usersConfig)
        }
      }

      def starting(programConfig: ProgramConfig): Unit = {
          val logPayment = context.spawn(LogPayment(programConfig.paymentConfig), "logPayment")
          val paymentsReader: ActorRef[PaymentsReaderOperation] = context.spawn(
            PaymentsReader(programConfig: ProgramConfig, logPayment: ActorRef[JournalOperation]), "paymentsReader")
          paymentsReader ! Start()
      }

      def terminateProgram(ex: Throwable): Unit = {
        context.log.error("Program error", ex)
        context.log.info("Terminating the actor system")
        context.system.terminate()
      }

      Behaviors.same
    }
}

object Main extends App {
  val system = ActorSystem(PaymentSystem(), "system")

  Thread.sleep(1000)
  system.terminate()
}
