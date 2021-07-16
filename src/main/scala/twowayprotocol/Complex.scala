package twowayprotocol

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.util.Timeout

import java.util.concurrent.TimeUnit
import scala.util.{Failure, Success}

object Complex extends App{
  val root: Behavior[Unit] = Behaviors.setup { context =>
    val b = context.spawn(B(), "b")
    val a = context.spawn(A(b), "a")
    a ! A.Request(456)
    a ! A.Request(789)
    Behaviors.same
  }
  ActorSystem(root, "ab-complex")

  private object A {
    sealed trait Message
    case class Request(value: Int) extends Message
    private case class BResponse(a: Int, b: Int) extends Message

    def apply(b: ActorRef[B.Message]): Behavior[Message] = {
      Behaviors.receive { (ctx, msg) =>
        msg match {
          case Request(value) =>
            ctx.log.info("Asking B for value... (A = {})", value)
            implicit val timeout: Timeout = Timeout(1, TimeUnit.SECONDS)
            ctx.ask(b, adapter => B.Request(adapter)) {
              case Success(response) => BResponse(value, response)
              case Failure(_) =>
                ctx.log.warn("B response timed out, using fallback value")
                BResponse(value, 0)
            }
          case BResponse(a, b) =>
            ctx.log.info("A = {}, B = {}", a, b)
        }
        Behaviors.same
      }
    }
  }

  private object B {
    sealed trait Message
    case class Request(ref: ActorRef[Int]) extends Message

    def apply(): Behavior[Message] =
      Behaviors.receiveMessage {
        case Request(ref) =>
          ref.tell(123)
          Behaviors.stopped
      }
  }
}
