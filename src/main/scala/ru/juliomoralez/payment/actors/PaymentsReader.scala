package ru.juliomoralez.payment.actors

import java.io.StreamCorruptedException
import java.nio.file.{Files, Path, Paths}

import akka.NotUsed
import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.dispatch.MessageDispatcher
import akka.event.LoggingAdapter
import akka.pattern.ask
import akka.stream.{ActorAttributes, Supervision}
import akka.stream.scaladsl.{FileIO, Framing, Source}
import akka.util.{ByteString, Timeout}
import ru.juliomoralez.payment.config.ProgramConfig
import ru.juliomoralez.payment.util.LoggerFactory

import scala.concurrent.duration.DurationInt
import scala.jdk.CollectionConverters.IteratorHasAsScala

case object Start

class PaymentsReader(implicit system: ActorSystem, log: LoggingAdapter, logPayment: ActorRef, programConfig: ProgramConfig) extends Actor with LoggerFactory {

  implicit val blockingExecutionContext: MessageDispatcher = system.dispatchers.lookup("blocking-dispatcher")
  implicit val timeout: Timeout = 5.seconds
  val fileDir: String = programConfig.paymentConfig.fileDir
  val fileRegex: String = programConfig.paymentConfig.fileRegex

  def receive: Receive = {
    case Start =>

      val paymentChecker: ActorRef = system.actorOf(Props(classOf[PaymentChecker], system, logPayment, programConfig), "paymentChecker")

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
          log.error("Incoming stream has incorrect element. Ignore")
          Supervision.Resume
        case ex: RuntimeException =>
          log.error(s"Incoming stream has RuntimeException: $ex. Stop")
          Supervision.Stop
        case ex =>
          log.error(s"Incoming stream has unhandled exception: $ex. Restart")
          Supervision.Restart
      }

      //основной стрим
      source
        .mapAsync(1)(x => paymentChecker.ask(CheckTransaction(x)))
        .addAttributes(ActorAttributes.supervisionStrategy(decider()))
        .run()

  }
}
