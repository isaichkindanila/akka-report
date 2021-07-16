import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, Behavior}

object Broadcasting {
  val PingKey: ServiceKey[Unit] = ServiceKey("ping-handler")

  def main(args: Array[String]): Unit = {
    ActorSystem(Broadcasting(), "broadcast")
  }

  def apply(): Behavior[Unit] =
    Behaviors.setup { context =>
      for (i <- 1 to 3) {
        context.spawn(pingHandler(), s"handler-$i")
      }
      context.spawn(Pinger(), "pinger") ! Pinger.Ping
      Behaviors.ignore
    }

  def pingHandler(): Behavior[Unit] =
    Behaviors.setup { context =>
      context.system.receptionist ! Receptionist.Register(PingKey, context.self)
      Behaviors.receive { (ctx, _) =>
        ctx.log.info("Got pinged!")
        Behaviors.same
      }
    }
}

object Pinger {
  sealed trait Message
  case object Ping extends Message
  private case class PingList(listing: Receptionist.Listing) extends Message

  def apply(): Behavior[Message] =
    Behaviors.setup { context =>

      val adapter = context.messageAdapter[Receptionist.Listing](PingList)
      Behaviors.receive { (ctx, msg) =>
        msg match {
          case Ping =>
            ctx.log.info("Requesting handlers...")
            ctx.system.receptionist ! Receptionist.Find(Broadcasting.PingKey, adapter)
          case PingList(Broadcasting.PingKey.Listing(listing)) =>
            ctx.log.info("Received {} handlers", listing.size)
            listing.foreach(handler => handler ! ())
        }
        Behaviors.same
      }
    }
}
