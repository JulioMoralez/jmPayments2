package ru.juliomoralez.payment.config

import com.typesafe.config.Config

final case class PaymentConfig(
    fileDir: String,
    fileRegex: String,
    paymentRegex: String,
    journalFilename: String,
    errorFilename: String)

object PaymentConfig extends Serializable {

  def apply(config: Config): PaymentConfig = {
    val fileDir: String = config.getString("app.file-dir")
    val fileRegex: String = config.getString("app.file-regex")
    val paymentRegex: String = config.getString("app.payment-regex")
    val journalFilename: String = config.getString("app.journal-file-name")
    val errorFilename: String = config.getString("app.error-file-name")
    new PaymentConfig(fileDir, fileRegex, paymentRegex, journalFilename, errorFilename)
  }
}
