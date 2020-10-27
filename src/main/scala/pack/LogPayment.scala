package pack

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths, StandardOpenOption}
import java.util.Date

import akka.actor.Actor

class LogPayment extends Actor{

  // два файла логов
  val errorFilename: String = "Error.log"
  val journalFilename: String = "Journal.log"

  def write(message: String, filename: String): Unit = {
    val path = Paths.get(filename)
    val option = if (Files.exists(path)) StandardOpenOption.APPEND else StandardOpenOption.CREATE
    Files.write(path, (message + "\n").getBytes(StandardCharsets.UTF_8), option)
  }

  override def preStart(): Unit = {
    val now:String = "\n" + new Date().toString
    write(now, errorFilename)
    write(now, journalFilename)
  }

  def receive: Receive = {
    case ErrorMessage(message) =>
      println("LogIncorrectPayment: " + message)
      write(message, errorFilename)
    case AddJournalMessage(message) =>
      println("Journal: " + message)
      write(message, journalFilename)
  }
}
