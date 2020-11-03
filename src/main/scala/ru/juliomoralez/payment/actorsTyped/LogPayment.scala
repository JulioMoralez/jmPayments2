package ru.juliomoralez.payment.actorsTyped

import java.util.Date

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import ru.juliomoralez.payment.config.PaymentConfig
import ru.juliomoralez.payment.util.FileUtils.writeToFile

sealed trait JournalOperation
case class ErrorMessage(message: String) extends JournalOperation
case class AddJournalMessage(message: String) extends JournalOperation

object LogPayment {

  def logPaymentOp(paymentConfig: PaymentConfig): Behavior[JournalOperation] = {

    val errorFilename: String = paymentConfig.errorFilename
    val journalFilename: String = paymentConfig.journalFilename
    val now:String = "\n" + new Date().toString
    writeToFile(now, errorFilename)
    writeToFile(now, journalFilename)

    Behaviors.receive { (context, message) =>

      def addMessage(text: String, filename: String): Behavior[JournalOperation] = {
        context.log.info(text)
        writeToFile(text, filename)
        Behaviors.same
      }

      message match {
        case ErrorMessage(text) => addMessage(text, errorFilename)
        case AddJournalMessage(text) => addMessage(text, journalFilename)
      }
    }
  }
}
