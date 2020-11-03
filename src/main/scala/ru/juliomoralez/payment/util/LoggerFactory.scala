package ru.juliomoralez.payment.util

import akka.actor.ActorSystem
import akka.event.{Logging, LoggingAdapter}


trait LoggerFactory {
  def newLogger(implicit actorSystem: ActorSystem): LoggingAdapter = Logging.getLogger(actorSystem, this)
}
