package my.tools.metric

import my.tools.metricCollector.Metric

trait MetricMixin {

  def gatherMetrics: () => Seq[String] = () => (
    for ((_,collector) <- collectors) yield collector()
  ).toSeq

  protected val collectors: Map[String,Metric]

}

object MetricMixin {
  case object QueryMetrics
  case class MetricsString(metric:String)
}