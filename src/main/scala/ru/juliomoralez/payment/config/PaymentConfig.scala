package ru.juliomoralez.payment.config

import com.typesafe.config.Config

final case class PaymentConfig(
    fileDir: String,
    fileRegex: String,
    paymentRegex: String,
    usersConfigFilepath: String)

object PaymentConfig extends Serializable {

  def apply(config: Config): PaymentConfig = {
    val fileDir = config.getString("app.file-dir")
    val fileRegex = config.getString("app.file-regex")
    val paymentRegex = config.getString("app.payment-regex")
    val usersConfigFilepath = config.getString("app.users-config-filepath")
    new PaymentConfig(fileDir, fileRegex, paymentRegex, usersConfigFilepath)
  }
}
