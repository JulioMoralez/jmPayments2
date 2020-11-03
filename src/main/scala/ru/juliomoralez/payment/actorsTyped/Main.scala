package ru.juliomoralez.payment.actorsTyped

import java.io.File

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import com.typesafe.config.ConfigFactory
import ru.juliomoralez.payment.Main.usersFileName
import ru.juliomoralez.payment.actorsTyped.LogPayment.logPaymentOp
import ru.juliomoralez.payment.actorsTyped.PaymentSystem.Init
import ru.juliomoralez.payment.actorsTyped.PaymentsReader.paymentsReaderOp
import ru.juliomoralez.payment.config.{PaymentConfig, ProgramConfig, UsersConfig}

import scala.util.Try


object PaymentSystem {
  case class Init()

  def apply(): Behavior[Init] =
    Behaviors.setup { context =>
      context.log.info("Program start")
      safeReadConfig().fold(terminateProgram, starting)

      def safeReadConfig(): Try[ProgramConfig] = {
        Try{
          val paymentConfig: PaymentConfig = PaymentConfig(ConfigFactory.load())
          val usersConfig = UsersConfig(ConfigFactory.parseFile(new File(usersFileName)))
          ProgramConfig(paymentConfig, usersConfig)
        }
      }

      def starting(programConfig: ProgramConfig): Unit = {
          val logPayment: ActorRef[JournalOperation] = context.spawn(logPaymentOp(programConfig.paymentConfig), "logPayment")
          val paymentsReader: ActorRef[PaymentsReaderOperation] = context.spawn(
            paymentsReaderOp(programConfig: ProgramConfig, logPayment: ActorRef[JournalOperation]), "paymentsReader")
          paymentsReader ! Start()
      }

      def terminateProgram(ex: Throwable): Unit = {
        context.log.error("Program error " + ex)
        context.log.info("Terminating the actor system")
        context.system.terminate()
      }

      Behaviors.same
    }
}

object Main extends App {
  val system: ActorSystem[PaymentSystem.Init] = ActorSystem(PaymentSystem(), "system")
  system ! Init()
}
