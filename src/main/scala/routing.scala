import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.{ActorSystem, Behavior}
import akka.actor.typed.scaladsl.{Behaviors, Routers}

object PoolRouting {

  def apply(): Behavior[Unit] =
    Behaviors.setup { context =>
      val poolRouter = Routers.pool[Unit](3) {
        Behaviors.receive { (ctx, _) =>
          ctx.log.info("Message received, stopping...")
          Behaviors.stopped
        }
      }

      val poolRef = context.spawn(poolRouter, "router")
      poolRef ! ()
      poolRef ! ()
      poolRef ! ()
      Behaviors.ignore
    }

  def main(args: Array[String]): Unit = {
    ActorSystem(PoolRouting(), "sys")
  }
}

object GroupRouting {

  def apply(): Behavior[Unit] =
    Behaviors.setup { context =>
      val key = ServiceKey[Unit]("worker")
      val worker = Behaviors.setup[Unit] { context =>
        context.system.receptionist ! Receptionist.Register(key, context.self)
        Behaviors.receive { (ctx, _) =>
          ctx.log.info("Received a message, yay!")
          Behaviors.same
        }
      }

      for (i <- 1 to 3) {
        context.spawn(worker, s"worker-$i")
      }

      val router = context.spawn(Routers.group(key), "router")
      router ! ()
      router ! ()
      router ! ()
      Behaviors.ignore
    }

  def main(args: Array[String]): Unit = {
    ActorSystem(GroupRouting(), "sys")
  }
}