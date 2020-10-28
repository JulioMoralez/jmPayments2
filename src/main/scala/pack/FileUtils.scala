package pack

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths, StandardOpenOption}

trait FileUtils {
  def writeToFile(message: String, filename: String): Unit = {
    val path = Paths.get(filename)
    val option = if (Files.exists(path)) StandardOpenOption.APPEND else StandardOpenOption.CREATE
    Files.write(path, (message + "\n").getBytes(StandardCharsets.UTF_8), option)
  }
}
