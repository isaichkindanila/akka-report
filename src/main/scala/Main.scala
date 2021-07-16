import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.{ActorRef, ActorSystem, Behavior, DispatcherSelector}
import buffer.BufferTest

object Main extends App {
  ActorSystem(BufferTest(50, 17), "buffer-test")

  def sendMessage[T](message: T, address: ActorRef[T]): Unit = {
    address.tell(message)
  }

  def spawnPinned[T](ctx: ActorContext[_],
                     behavior: Behavior[T],
                     name: String): ActorRef[T] = {
    val pinned = DispatcherSelector.fromConfig("custom-pinned-dispatcher")
    ctx.spawn(behavior, name, pinned)
  }
}
