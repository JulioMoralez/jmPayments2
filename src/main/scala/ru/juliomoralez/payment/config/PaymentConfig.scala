package ru.juliomoralez.payment.config

import com.typesafe.config.{Config, ConfigFactory}

import scala.util.Try
import scala.util.matching.Regex

object PaymentConfig {
  val config: Config = ConfigFactory.load()

  // в случае отсутствия полей в application.conf используем значения по умолчанию и продолжаем работу
  val errorFilename: String = get("errorFilename").getOrElse("Error.log")
  val journalFilename: String = get("journalFilename").getOrElse("Journal.log")
  val dir: String = get("dir").getOrElse(".")
  val fileFilter: String = get("fileFilter").getOrElse("")
  val regex: Regex = get("regex").getOrElse("").r

  // чтение поля из файла application.conf
  def get(field: String): Try[String] = {
    Try {
      config.getString("app." + field)
    }
  }

}
