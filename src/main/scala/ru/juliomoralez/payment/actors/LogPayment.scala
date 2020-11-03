package ru.juliomoralez.payment.actors

import java.util.Date

import akka.actor.Actor
import akka.event.LoggingAdapter
import ru.juliomoralez.payment.config.ProgramConfig
import ru.juliomoralez.payment.util.FileUtils.writeToFile

case class ErrorMessage(message: String)
case class AddJournalMessage(message: String)

class LogPayment(log: LoggingAdapter, programConfig: ProgramConfig) extends Actor {

  // два файла логов
  val errorFilename: String = programConfig.paymentConfig.errorFilename
  val journalFilename: String = programConfig.paymentConfig.journalFilename

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
