package lila.round

import akka.actor._
import kamon._
import metric.SubscriptionsDispatcher.TickMetricSnapshot
import scala.concurrent.duration._

private object MoveMonitor {

  def start(system: ActorSystem, channel: lila.socket.Channel) =

    Kamon.metrics.subscribe("trace", "round.move.trace", system.actorOf(Props(new Actor {
      var currentMicros: Int = 0
      context.system.scheduler.schedule(5 second, 2 second) {
        channel ! lila.socket.Channel.Publish(lila.socket.Socket.makeMessage(
          "mlat",
          (currentMicros / 100) / 10d
        ))
        system.lilaBus.publish(lila.hub.actorApi.round.Mlat(currentMicros), 'mlat)
      }
      def receive = {
        case tick: TickMetricSnapshot => tick.metrics.collectFirst {
          case (entity, snapshot) if entity.category == "trace" => snapshot
        } flatMap (_ histogram "elapsed-time") foreach { h =>
          if (!h.isEmpty) currentMicros = Math.round(h.sum / h.numberOfMeasurements / 1000).toInt
        }
      }
    })))
}
