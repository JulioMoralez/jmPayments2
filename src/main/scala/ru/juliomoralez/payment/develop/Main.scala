package ru.juliomoralez.payment.develop

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.stream.scaladsl.Source

object User {
  final case class Init(name: String)

  def apply(): Behavior[Init] =
    Behaviors.receive { (context, message) =>
    println(context.self.path + " " +  message.name)
    Behaviors.same
  }
}

object Reader {
  final case class Start()

  def apply(): Behavior[Start] =
    Behaviors.setup { context =>
      Behaviors.receiveMessage { _ =>
        def createActor(name: String, i : Int): Unit = {
          val user: ActorRef[User.Init] = context.spawn(User(), name + i)
          user ! User.Init("i=" + (i * i))
        }
        (1 to 3).foreach(createActor("a", _)) // нормально создаются 3 штуки
        implicit val c: ActorSystem[Nothing] = context.system
        Source(1 to 3).runForeach(x => {println(x); createActor("b", x)}) // печатает один раз x и останавливается на spawn
        println("end")
        Behaviors.same
      }
    }
}

object Main extends App {
  val system: ActorSystem[Reader.Start] = ActorSystem(Reader(), "system")
  system ! Reader.Start()

  Thread.sleep(1000)
  system.terminate()
}
