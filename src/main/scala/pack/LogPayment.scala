package pack

import java.util.Date

import akka.actor.Actor
import akka.event.{Logging, LoggingAdapter}

case class ErrorMessage(message: String)
case class AddJournalMessage(message: String)

class LogPayment extends Actor with FileUtils {

  // два файла логов
  val errorFilename: String = MyConfiguration.errorFilename
  val journalFilename: String = MyConfiguration.journalFilename
  val log: LoggingAdapter = Logging(context.system, this)

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
