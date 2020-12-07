package ru.juliomoralez.payment.actorsTyped

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.stream.scaladsl.{FileIO, Framing}
import akka.stream.{ActorAttributes, Supervision}
import akka.util.ByteString
import ru.juliomoralez.payment.actorsTyped.LogPayment.JournalOperation
import ru.juliomoralez.payment.actorsTyped.PaymentChecker.CheckPayment
import ru.juliomoralez.payment.config.ProgramConfig

import java.io.StreamCorruptedException
import java.nio.file.{Files, Path, Paths}
import scala.util.Try

object PaymentsReader extends Serializable {
  sealed trait PaymentsReaderOperation
  final case class Start() extends PaymentsReaderOperation

  def apply(implicit programConfig: ProgramConfig, logPayment: ActorRef[JournalOperation]): Behavior[PaymentsReaderOperation] = {
    Behaviors.receive { (context, message) =>
      message match {
        case Start() => start(context)
      }
    }
  }

  def start(context :ActorContext[PaymentsReaderOperation])
           (implicit programConfig: ProgramConfig, logPayment: ActorRef[JournalOperation]): Behavior[PaymentsReaderOperation] = {
    val fileDir = programConfig.paymentConfig.fileDir
    val fileRegex = programConfig.paymentConfig.fileRegex

    getFile(fileDir, fileRegex) match {
      case Some(path) =>
        val source = FileIO
          .fromPath(path)
          .via(Framing.delimiter(ByteString(System.lineSeparator()), 128, allowTruncation = true)
            .map(_.utf8String)).filter(_.nonEmpty)

        val paymentChecker = context.spawn(PaymentChecker(programConfig: ProgramConfig, logPayment: ActorRef[JournalOperation]), "paymentChecker")
        implicit val materializer: ActorSystem[Nothing] = context.system
        source
          .map(paymentChecker ! CheckPayment(_))
          .addAttributes(ActorAttributes.supervisionStrategy(decider(context)))
          .run()

      case None =>
        context.log.error(s"File not found in directory `$fileDir` with mask `$fileRegex` ")
    }
    Behaviors.same
  }

  def getFile(fileDir: String, fileRegex: String): Option[Path] = {
    Try(Files
      .list(Paths.get(fileDir))
      .filter(_.getFileName.toString.matches(fileRegex))
      .findFirst().get()).toOption
  }

  def decider(context :ActorContext[PaymentsReaderOperation]): Supervision.Decider = {
    case _: StreamCorruptedException =>
      context.log.error("Incoming stream has incorrect element. Ignore")
      Supervision.Resume
    case ex: RuntimeException =>
      context.log.error(s"Incoming stream has RuntimeException: $ex. Stop")
      Supervision.Stop
    case ex =>
      context.log.error(s"Incoming stream has unhandled exception: $ex. Restart")
      Supervision.Restart
  }
}




