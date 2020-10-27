package pack

import java.io.File
import java.nio.file.Path

import com.typesafe.config.{Config, ConfigFactory}

import scala.util.{Failure, Success, Try}

object MyConfiguration {
  val config: Config = ConfigFactory.load()

  // чтение поля из файла application.conf
  def get(field: String): Try[String] = {
    Try {
      config.getString("app." + field)
    }
  }

  // выбираем файлы из папки dir по маске filter
  def files: Vector[Path] = { //
    val dir: String = get("dir") match {
      case Success(value) => value
      case Failure(_) => "."
    }
    val filter: String = get("filter") match {
      case Success(value) => value
      case Failure(_) => ""
    }
    val files: Array[File] = new File(dir).listFiles()
    files.filter(x => x.isFile && x.getName.indexOf(filter) >= 0).map(x => x.toPath).toVector
  }
}
