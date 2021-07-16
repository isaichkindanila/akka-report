package buffer

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}

import scala.concurrent.duration.DurationInt

object Buffer {
  // Общий тип принимаемых сообщений.
  sealed trait Message

  // Отправка данных в буфер
  case class Send[T](item: T) extends Message

  // Отправка буфера по адресу назначения; вызывается клиентом.
  case object Flush extends Message

  // Отправка буфера по адресу назначения; вызывается внутренним таймером.
  private case object Timeout extends Message

  // Ключ таймера, необходим для контроля срока буферизации данных.
  private object TimerKey

  // Фабричный метод Актора, отвечающего за буферизации сообщений.
  def apply[T](bufferSize: Int, target: ActorRef[Vector[T]]): Behavior[Message] =
    new Buffer[T](bufferSize, target).idle()
}

// Неизменяемый класс, имплементирующий логику буферизации.
private class Buffer[T](maxBufferSize: Int, target: ActorRef[Vector[T]]) {
  // Неактивное состояние Актора; переходит в активное при отправке данных в буфер.
  // В этом состоянии Актор игнорирует все сообщения, кроме Send.
  private def idle(): Behavior[Buffer.Message] =
    Behaviors.receiveMessagePartial {
      case msg: Buffer.Send[T] => active(Vector(msg.item))
    }

  // Активное состояние Актора, буферизирует присланные данные и
  // отсылает их по адресу назначения при получении сообщения Flush
  // либо после одной секунды с момента последнего поступления данных.
  // При отправке буфера Актор возвращается в неактивное состояние.
  private def active(buffer: Vector[T]): Behavior[Buffer.Message] = {
    Behaviors.withTimers { timers =>
      // Создание или обновление таймера; посылает Timeout самому себе
      // по истечению указанного срока (если таймер не будет обновлён).
      timers.startSingleTimer(Buffer.TimerKey, Buffer.Timeout, 1.second)
      Behaviors.receiveMessage {
        // Flush и Timeout вызывают отправку буфера если тот не пуст.
        case Buffer.Flush | Buffer.Timeout =>
          if (buffer.nonEmpty) {
            target.tell(buffer)
          }
          idle()                // переход в неактивное состояние
        // Send вызывает отправку буфера только при его заполнении.
        case msg: Buffer.Send[T] =>
          val newBuffer = buffer :+ msg.item
          if (newBuffer.size >= maxBufferSize) {
            target.tell(newBuffer)
            idle()              // переход в неактивное состояние
          } else
            active(newBuffer)   // переход в активное состояние с изменённым буфером
      }
    }
  }
}
