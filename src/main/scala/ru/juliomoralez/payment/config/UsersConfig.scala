package ru.juliomoralez.payment.config

import java.io.File

import com.typesafe.config.{Config, ConfigFactory}

import scala.jdk.CollectionConverters.CollectionHasAsScala

object UsersConfig {
  val config: Config = ConfigFactory.parseFile(new File("src/main/resources/users.conf"))
  val usersStartValue: Map[String, Long] = config.getConfigList("users").asScala
    .map(u => u.getString("name").trim -> u.getLong("balance")).toMap
}
