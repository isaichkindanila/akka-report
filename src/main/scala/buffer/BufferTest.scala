package buffer

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors

object BufferTest {
  def apply(total: Int, batchSize: Int): Behavior[Vector[Int]] =
  // Для создания Актора буфера необходим контекст, используется фабричный метод
    Behaviors.setup { context =>
      // Создание буфера с указанием собственного адреса в качестве назначения
      // ActorContext.spawn возвращает ActorRef созданного Актора
      val buffer = context.spawn(Buffer(batchSize, context.self), "test-buffer")
      for (i <- 1 to total) {
        buffer ! Buffer.Send(i)
      }

      var received = 0
      Behaviors.receive { (ctx, batch) =>
        ctx.log.info("Received a batch (size = {})", batch.size)
        received += batch.size

        // Останавливаем Актор как только буфер прислал все сообщения
        // В противном случае возвращаем этот же Behavior
        if (received >= total)
          Behaviors.stopped
        else
          Behaviors.same
      }
    }
}
