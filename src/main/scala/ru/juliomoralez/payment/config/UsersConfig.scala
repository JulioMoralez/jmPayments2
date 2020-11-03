package ru.juliomoralez.payment.config

import com.typesafe.config.Config

import scala.jdk.CollectionConverters.CollectionHasAsScala

final case class UsersConfig(usersStartValue: Map[String, Long])

object UsersConfig {
  def apply(config: Config): UsersConfig = {
    val usersStartValue: Map[String, Long] = config.getConfigList("users").asScala
      .map(u => u.getString("name").trim -> u.getLong("balance")).toMap
    new UsersConfig(usersStartValue)
  }
}
