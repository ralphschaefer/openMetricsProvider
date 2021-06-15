package my.tools.metricCollector

import akka.actor.{Actor, ActorContext, ActorLogging, ActorRef, ActorSystem, Props}
import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.scalalogging.StrictLogging
import my.tools.metricCollector.Metric.context

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.DurationInt
import scala.collection.mutable

class Gauge(metricContext: Metric.context) extends MetricCollector(metricContext) {
  import Gauge._

  private val value: mutable.Map[Metric.context, Double] = mutable.Map.empty[context, Double]

  override def receive: Receive = {
    case Init =>
      value(metricContext) = 0.0
    case Inc(inc, ctx) if ctx.nonEmpty =>
      val combinedCtx = metricContext ++ ctx
      value(combinedCtx) = inc + value.getOrElse[Double](combinedCtx, 0.0)
    case Inc(inc, _) =>
      value(metricContext) = inc + value.getOrElse[Double](metricContext, 0.0)
    case Dec(dec, ctx) if ctx.nonEmpty =>
      val combinedCtx = metricContext ++ ctx
      value(combinedCtx) = value.getOrElse[Double](combinedCtx, 0.0) - dec
    case Dec(dec, _) =>
      value(metricContext) = value.getOrElse[Double](metricContext, 0.0) - dec
    case Set(v, ctx) if ctx.nonEmpty =>
      val combinedCtx = metricContext ++ ctx
      value(combinedCtx) = v
    case Set(v, _) =>
      value(metricContext) = v
    case MetricCollector.Query =>
      sender() ! QueryResults(value.toMap)
  }

}

object Gauge {

  def builder(
               name:String, description:String,
               metricContext: Metric.context)(implicit system: ActorSystem): Metric = {
    import system.dispatcher
    val collectorActor =
      system.actorOf(Props(new Gauge(metricContext)), name+"_gauge_collector_" + java.util.UUID.randomUUID.toString)
    new GaugeMetric(collectorActor, name, description)
    val m = new GaugeMetric(collectorActor, name, description)
    m.collector ! Init
    m
  }

  sealed trait Probe
  private case object Init extends Probe
  case class Inc(value:Double, metricContext: Metric.context=Map()) extends Probe
  case class Dec(value:Double, metricContext: Metric.context=Map()) extends Probe
  case class Set(value:Double, metricContext: Metric.context=Map()) extends Probe
  case class QueryResults(counts: Map[Metric.context,Double])


  class GaugeMetric(gaugeActor: ActorRef, name:String, description:String)(implicit executor:ExecutionContext)
    extends Metric(gaugeActor, name, description)
    with StrictLogging {
    override def metricType = "gauge"
    implicit val timeout = Timeout(500.millis)

    def metric:String = {
      val f = (gaugeActor ? MetricCollector.Query).map {
        case QueryResults(values) =>
          Some(
            (for((k,v) <- values) yield s"$name${Metric.writeContext(k)} ${v}").mkString("\n")
          )
        case other =>
          logger.error(s"unexpected: $other")
          None
      }.fallbackTo(Future(None)).filter(_.nonEmpty).map(_.get)
      Await.result(f, timeout.duration)
    }

  }

  def props(metricContext: Metric.context) = Props(new Gauge(metricContext))

}