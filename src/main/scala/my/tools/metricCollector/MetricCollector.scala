package my.tools.metricCollector

import akka.actor.{Actor, ActorLogging}

abstract class MetricCollector(val metricContext: Metric.context) extends Actor with ActorLogging

object MetricCollector {
  case object Query
}
