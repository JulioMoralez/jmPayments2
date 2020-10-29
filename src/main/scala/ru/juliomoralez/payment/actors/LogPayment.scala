package ru.juliomoralez.payment.actors

import java.util.Date

import akka.actor.Actor
import akka.event.LoggingAdapter
import ru.juliomoralez.payment.config.PaymentConfig
import ru.juliomoralez.payment.util.FileUtils.writeToFile
import ru.juliomoralez.payment.util.LoggerFactory

case class ErrorMessage(message: String)
case class AddJournalMessage(message: String)

class LogPayment extends Actor with LoggerFactory {

  // два файла логов
  val errorFilename: String = PaymentConfig.errorFilename
  val journalFilename: String = PaymentConfig.journalFilename
  val log: LoggingAdapter = newLogger(context.system)

  override def preStart(): Unit = {
    val now:String = "\n" + new Date().toString
    writeToFile(now, errorFilename)
    writeToFile(now, journalFilename)
  }

  def receive: Receive = {
    case ErrorMessage(message) =>
      log.error(message)
      writeToFile(message, errorFilename)
    case AddJournalMessage(message) =>
      log.info(message)
      writeToFile(message, journalFilename)
  }
}
