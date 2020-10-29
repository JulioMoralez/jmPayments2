package ru.juliomoralez.payment.config

import java.io.File

import com.typesafe.config.{Config, ConfigFactory}

import scala.jdk.CollectionConverters.CollectionHasAsScala

final case class User(name: String, balance: Long)

object UserConfig {
  val config: Config = ConfigFactory.parseFile(new File("src/main/resources/users.conf"))
  val usersStartValue: Map[String, Long] = config.getConfigList("users").asScala
    .map(u => User(u.getString("name"), u.getLong("balance")))
    .map(u => u.name.trim -> u.balance).toMap
}
