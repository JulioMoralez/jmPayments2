package pack

import com.typesafe.config.{Config, ConfigFactory}

import scala.jdk.CollectionConverters.CollectionHasAsScala
import scala.util.Try

object MyConfiguration {
  val config: Config = ConfigFactory.load()

  val errorFilename: String = get("errorFilename").getOrElse("Error.log")
  val journalFilename: String = get("journalFilename").getOrElse("Journal.log")
  val dir: String = get("dir").getOrElse(".")
  val fileFilter: String = get("fileFilter").getOrElse("")

  // читаем и делаем мапу стартовых сумм пользователей
  val usersStartValue: Map[String, Long] =
    config.getStringList("app.users").asScala.toVector
    .filter(_.matches("[\\w]+: [ \\d]+"))
    .map(x => x.split(":"))
    .filter(x => Try(x(1).trim.toLong).isSuccess)
    .map(x => x(0).trim -> x(1).trim.toLong).toMap

  // чтение поля из файла application.conf
  def get(field: String): Try[String] = {
    Try {
      config.getString("app." + field)
    }
  }

}
