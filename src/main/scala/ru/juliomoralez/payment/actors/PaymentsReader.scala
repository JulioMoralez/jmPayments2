package ru.juliomoralez.payment.actors

import java.io.File
import java.nio.file.Path

import akka.NotUsed
import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.dispatch.MessageDispatcher
import akka.pattern.ask
import akka.stream.scaladsl.{FileIO, Framing, Source}
import akka.util.{ByteString, Timeout}
import ru.juliomoralez.payment.config.PaymentConfig.{dir, fileFilter}

import scala.collection.mutable
import scala.concurrent.duration.DurationInt
import scala.util.Try

case object Start

class PaymentsReader(implicit system: ActorSystem, logPayment: ActorRef) extends Actor {

  implicit val blockingExecutionContext: MessageDispatcher = system.dispatchers.lookup("blocking-dispatcher")
  implicit val timeout: Timeout = 5.seconds
  val users: mutable.Map[String, ActorRef] = mutable.Map()

  def receive: Receive = {
    case Start =>

      // проверка входной строки на <NAME1> -> <NAME2>: <VALUE>
      def paymentChecker(payment: String): Boolean = {
        val checked: Boolean = payment.matches("[\\w]+ -> [\\w]+: [ \\d]+")
        if (checked) {
          logPayment ! AddJournalMessage(payment)
        } else {
          logPayment ! ErrorMessage(s"$payment - ошибка в строке")
        }
        checked
      }

      // получаем из <NAME1> -> <NAME2>: <VALUE>
      def parsePayment(s: String): (ActorRef, Payment) = {
          val sep1: Int = s.indexOf("->")
          val sep2: Int = s.indexOf(":")
          val p1: String = s.substring(0, sep1).trim
          val p2: String = s.substring(sep1 + 2, sep2).trim
          val value: Long = Try {
            s.substring(sep2 + 1, s.length).trim.toLong
          }.getOrElse(-1) // -1 если не удалось получить сумму из операции

          def createActor(name: String): Unit = {
            if (!users.contains(name)) {
              val paymentParticipant: ActorRef = system.actorOf(Props(classOf[PaymentParticipant], logPayment), name)
              users += (name -> paymentParticipant)
            }
          }
          createActor(p1)
          createActor(p2)

        (users(p1), Payment(Minus, value, users(p2)))
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
      source.filter(paymentChecker).mapAsync(1)(p => {
        val (user, payment): (ActorRef, Payment) = parsePayment(p)
        if (payment.value >= 0) user.ask(payment) else logPayment.ask(AddJournalMessage("сумма в операции некорректна"))
      }).run()

  }
}
