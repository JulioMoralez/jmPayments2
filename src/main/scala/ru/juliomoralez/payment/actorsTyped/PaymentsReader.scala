package ru.juliomoralez.payment.actorsTyped

import java.io.StreamCorruptedException
import java.nio.file.{Files, Path, Paths}

import akka.NotUsed
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.stream.scaladsl.{FileIO, Framing, Source}
import akka.stream.{ActorAttributes, Supervision}
import akka.util.ByteString
import ru.juliomoralez.payment.config.ProgramConfig

import scala.jdk.CollectionConverters.IteratorHasAsScala

sealed trait PaymentsReaderOperation
case class Start() extends PaymentsReaderOperation

object PaymentsReader {

  def apply(programConfig: ProgramConfig, logPayment: ActorRef[JournalOperation]): Behavior[PaymentsReaderOperation] = {
    Behaviors.receive { (context, message) =>

      def start(): Behavior[PaymentsReaderOperation] = {
        val fileDir: String = programConfig.paymentConfig.fileDir
        val fileRegex: String = programConfig.paymentConfig.fileRegex
        val paymentChecker: ActorRef[String] = context.spawn(PaymentChecker(programConfig: ProgramConfig, logPayment: ActorRef[JournalOperation]), "paymentChecker")

        def getFiles(fileDir: String, fileRegex: String): Iterator[Path] = {
          Files
            .list(Paths.get(fileDir))
            .filter(_.getFileName.toString.matches(fileRegex))
            .iterator()
            .asScala
        }

        val source: Source[String, NotUsed] = Source
          .fromIterator(() => getFiles(fileDir, fileRegex))
          .flatMapConcat(
            FileIO.fromPath(_).via(Framing.delimiter(ByteString(System.lineSeparator()), 128, allowTruncation = true)
              .map(_.utf8String)).filter(!_.isEmpty))

        def decider(): Supervision.Decider = {
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

        //основной стрим
        implicit val system: ActorSystem[Nothing] = context.system
        source
          .map(paymentChecker ! _)
          .addAttributes(ActorAttributes.supervisionStrategy(decider()))
          .run()

        Behaviors.same
      }

      message match {
        case Start() => start()
      }
    }
  }



}
