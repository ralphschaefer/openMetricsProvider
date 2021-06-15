package my.tools.metricCollector

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.scalalogging.StrictLogging
import my.tools.metricCollector.Metric.context

import scala.collection.mutable
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.DurationInt


class Count(metricContext: Metric.context) extends MetricCollector(metricContext) {
  import Count._

  private val count: mutable.Map[Metric.context, Long] = mutable.Map.empty[context, Long]
  override def receive: Receive = {
    case Init =>
      count(metricContext) = 0
    case Inc =>
      count(metricContext) = 1 + count.getOrElse[Long](metricContext, 0)
    case Inc(ctx) =>
      val combinedCtx = metricContext ++ ctx
      count(combinedCtx) = 1 + count.getOrElse[Long](combinedCtx, 0)
    case MetricCollector.Query =>
      sender() ! QueryResults(count.toMap)
  }

}

object Count {

  def builder(
               name:String, description:String,
               metricContext: Metric.context)(implicit system: ActorSystem): Metric = {
    import system.dispatcher
    val collectorActor =
      system.actorOf(Props(new Count(metricContext)), name+"_count_collector_" + java.util.UUID.randomUUID.toString)
    val m = new CountMetric(collectorActor, name, description)
    m.collector ! Init
    m
  }

  sealed trait Probe
  private case object Init extends Probe
  case object Inc extends Probe
  case class Inc(metricContext: Metric.context) extends Probe
  case class QueryResults(counts: Map[Metric.context,Long])

  class CountMetric(countActor: ActorRef, name:String, description:String)(implicit executor:ExecutionContext)
    extends Metric(countActor, name, description)
    with StrictLogging {
    override def metricType = "counter"
    implicit val timeout = Timeout(500.millis)

    def metric: String = {
      val f = (countActor ? MetricCollector.Query).map{
        case QueryResults(counts) =>
          Some(
            (for((k,v) <- counts) yield s"$name${Metric.writeContext(k)} ${v.toDouble}").mkString("\n")
          )
        case other =>
          logger.error(s"unexpected: $other")
          None
      }.fallbackTo(Future(None)).filter(_.nonEmpty).map(_.get)
      Await.result(f, timeout.duration)
    }


  }

  def props(metricContext: Metric.context) = Props(new Count(metricContext))

}