package my.tools.metricCollector

import akka.actor.ActorRef


abstract class Metric(val collector: ActorRef, val name: String, description: String) {
  def metricType: String
  val NotAvailable = "n/a"
  val metricPrefix = s"# HELP $name $description\n# TYPE $name $metricType\n"
  def metric:String
  def apply() = metricPrefix + metric
  def entry:Map[String,Metric] = Map(name -> this)
}

object Metric {
  type context = Map[String, String]
  val emptyContext: context = Map()
  def writeContext(c: context):String =
    if (c.nonEmpty)
      "{" + (for ((k,v) <- c) yield s"""$k="$v"""").mkString(",") + ",}"
    else ""
}