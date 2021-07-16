package twowayprotocol

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}

object Simple extends App {
  val root: Behavior[Unit] = Behaviors.setup { context =>
    val b = context.spawn(B(), "b")
    val a = context.spawn(A(b), "a")
    a ! A.Request()
    Behaviors.same
  }
  ActorSystem(root, "ab-sys")

  private object A {
    sealed trait Message
    case class Request() extends Message
    private case class BResponse(value: Int) extends Message

    private def adapt(target: ActorRef[Message]): Behavior[Int] =
      Behaviors.receiveMessage { value =>
        target ! BResponse(value)
        Behaviors.same
      }

    def apply(b: ActorRef[B.Message]): Behavior[Message] = {
      Behaviors.setup { context =>
        val adapter = context.spawn(adapt(context.self), "b-adapter")
        Behaviors.receive { (ctx, msg) =>
          msg match {
            case Request() =>
              ctx.log.info("Asking B for value...")
              b ! B.Request(adapter)
            case BResponse(value) =>
              ctx.log.info("B sent {}", value)
          }
          Behaviors.same
        }
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
          Behaviors.same
      }
  }
}
