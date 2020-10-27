package pack

import akka.actor.{Actor, ActorRef, Props}
import pack.Main.{logPayment, persons, system}

class PaymentChecker() extends Actor{
  def receive: Receive = {
    case CheckPayment(payment1) => {
      var checked: Boolean = false
      var errorMessage: String = ""
      var p1: String = ""
      var p2: String = ""
      var p3: String = ""
      var value: Long = 0
      val arrow1:Int = payment1.indexOf("->")
      if (arrow1 > 0) {
        p1 = payment1.substring(0, arrow1).trim
        val payment2 = payment1.substring(arrow1 + 2, payment1.length)
        val arrow2:Int = payment2.indexOf(":")
        if (arrow2 > 0) {
          p2 = payment2.substring(0, arrow2).trim
          p3 = payment2.substring(arrow2 + 1, payment2.length).trim
          val regex: String = "[\\w]+"
          if (p1.matches(regex)) {
            if (p2.matches(regex)) {
              try {
                value = p3.toLong
                checked = true
              } catch {
                case _: Exception => errorMessage = "поле сумма с ошибкой"
              }
            } else {
              errorMessage = "имя получателя с ошибкой"
            }
          } else {
            errorMessage = "имя отправителя с ошибкой"
          }
        } else {
          errorMessage = "нет символа :"
        }
      } else {
        errorMessage = "нет символа ->"
      }
      if (checked) { // операция записана правильно
        // создаем нового участника, если ещё не было его
        def createActor(name: String): Unit = {
          if (!persons.contains(name)) {
            val paymentParticipant: ActorRef = system.actorOf(Props(classOf[PaymentParticipant]), name)
            persons += (name -> (paymentParticipant))
            println(name + " created")
          }
        }
        createActor(p1)
        createActor(p2)
        logPayment ! AddJournalMessage(payment1)
        persons(p1) ! Payment(PaymentSign("-"), value, persons(p2))
        Thread.sleep(10) // !!!!!!!!!!
      } else {
        logPayment ! ErrorMessage(s"$payment1 - $errorMessage")
      }
    }
  }

}
