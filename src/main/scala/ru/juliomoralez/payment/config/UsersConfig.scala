package ru.juliomoralez.payment.config

import com.typesafe.config.Config

import scala.jdk.CollectionConverters.CollectionHasAsScala

final case class UsersConfig(
          defaultUserBalance: Long,
          usersStartBalance: Map[String, Long])

object UsersConfig extends Serializable{
  def apply(config: Config): UsersConfig = {
    val defaultUserBalance = config.getLong("default-user-value")
    val usersStartBalance = config.getConfigList("users").asScala
      .map(u => u.getString("name").trim -> u.getLong("balance")).toMap
    new UsersConfig(defaultUserBalance, usersStartBalance)
  }
}
