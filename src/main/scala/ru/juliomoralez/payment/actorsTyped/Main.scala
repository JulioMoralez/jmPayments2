package ru.juliomoralez.payment.actorsTyped

import java.io.File

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import com.typesafe.config.ConfigFactory
import ru.juliomoralez.payment.config.{PaymentConfig, ProgramConfig, UsersConfig}
import ru.juliomoralez.payment.util.Const.USERS_CONFIG_FILE_PATH

import scala.util.Try


object PaymentSystem {

  def apply(): Behavior[Any] =
    Behaviors.setup { context =>
      context.log.info("Program start")
      safeReadConfig().fold(terminateProgram, starting)

      def safeReadConfig(): Try[ProgramConfig] = {
        Try{
          val paymentConfig: PaymentConfig = PaymentConfig(ConfigFactory.load())
          val usersConfig = UsersConfig(ConfigFactory.parseFile(new File(USERS_CONFIG_FILE_PATH)))
          ProgramConfig(paymentConfig, usersConfig)
        }
      }

      def starting(programConfig: ProgramConfig): Unit = {
          val logPayment: ActorRef[JournalOperation] = context.spawn(LogPayment(programConfig.paymentConfig), "logPayment")
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
