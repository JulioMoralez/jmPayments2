package pack

import akka.NotUsed
import akka.actor.{Actor, ActorSystem}
import akka.stream.scaladsl.{FileIO, Framing, Sink, Source}
import akka.util.ByteString
import pack.Main.paymentChecker

class PaymentsReader(implicit system: ActorSystem) extends Actor {


  def receive: Receive = {
    // читаем из файлов операции
    case Start => {
      val sources = MyConfiguration.files
        .map(p => {
          FileIO
            .fromPath(p)
            .via(Framing.delimiter(ByteString(System.lineSeparator()), Int.MaxValue, allowTruncation = true)
              .map(_.utf8String)).filter(x => !x.isEmpty)
        })

      val sink = Sink.foreach[String](x => paymentChecker ! CheckPayment(x))

      val value: Source[String, NotUsed] = Source(sources).flatMapConcat(identity)

      value.to(sink).run()
    }

  }
}
