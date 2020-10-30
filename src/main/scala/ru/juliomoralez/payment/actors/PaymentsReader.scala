package ru.juliomoralez.payment.actors

import java.io.File
import java.nio.file.Path

import akka.NotUsed
import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.dispatch.MessageDispatcher
import akka.pattern.ask
import akka.stream.scaladsl.{FileIO, Framing, Source}
import akka.util.{ByteString, Timeout}
import ru.juliomoralez.payment.config.PaymentConfig.{dir, fileFilter, regex}

import scala.collection.mutable
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

case object Start

sealed trait Transaction
case class GoodTransaction(from: String, to: String, value: Long) extends Transaction
case object BadTransaction extends Transaction

class PaymentsReader(implicit system: ActorSystem, logPayment: ActorRef) extends Actor {

  implicit val blockingExecutionContext: MessageDispatcher = system.dispatchers.lookup("blocking-dispatcher")
  implicit val timeout: Timeout = 5.seconds
  val users: mutable.Map[String, ActorRef] = mutable.Map()

  def receive: Receive = {
    case Start =>

      // проверка входной строки на <NAME1> -> <NAME2>: <VALUE>
      def paymentChecker(payment: String): Transaction = {
        payment match {
          case regex(from, _, to, _, value) =>
            logPayment ! AddJournalMessage(payment)
            GoodTransaction(from, to, value.toLong)
          case _ =>
            logPayment ! ErrorMessage(s"$payment - ошибка в строке")
            BadTransaction
        }
      }

      def executePayment(p: Transaction): Future[Any] = {
          def createActor(name: String): Unit = {
            if (!users.contains(name)) {
              val paymentParticipant: ActorRef = system.actorOf(Props(classOf[PaymentParticipant], logPayment), name)
              users += (name -> paymentParticipant)
            }
          }
          p match {
            case GoodTransaction(from, to, value) =>
              createActor(from)
              createActor(to)
              users(from).ask(Payment(Minus, value, users(to)))
          }

      }

      // выбираем файлы из папки dir по маске filter
      def files: Vector[Path] = {
        val files: Array[File] = new File(dir).listFiles()
        if (files != null) {
          files.filter(x => x.isFile && x.getName.indexOf(fileFilter) >= 0).map(_.toPath).toVector
        } else { // Что лучше делать в случае ошибки чтения? аварийно остановить всё?
          // throw new RuntimeException("Ошибка чтения исходных файлов с операциями")
          Vector()
        }
      }

      val sources = files
        .map(
          FileIO
            .fromPath(_)
            .via(Framing.delimiter(ByteString(System.lineSeparator()), Int.MaxValue, allowTruncation = true)
              .map(_.utf8String)).filter(!_.isEmpty)
        )
      val source: Source[String, NotUsed] = Source(sources).flatMapConcat(identity)

      //основной стрим
      source
        .map(paymentChecker)
        .filter(_ != BadTransaction)
        .mapAsync(1)(executePayment)
        .run()

  }
}
