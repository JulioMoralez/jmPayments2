package ru.juliomoralez.payment.config

import com.typesafe.config.Config

import scala.jdk.CollectionConverters.CollectionHasAsScala

final case class UsersConfig(
          defaultUserValue: Long,
          usersStartValue: Map[String, Long])

object UsersConfig {
  def apply(config: Config): UsersConfig = {
    val defaultUserValue: Long = config.getLong("default-user-value")
    val usersStartValue: Map[String, Long] = config.getConfigList("users").asScala
      .map(u => u.getString("name").trim -> u.getLong("balance")).toMap
    new UsersConfig(defaultUserValue, usersStartValue)
  }
}
