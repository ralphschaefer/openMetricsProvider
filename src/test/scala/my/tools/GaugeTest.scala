package my.tools

import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatest.BeforeAndAfterAll
import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender,TestKit}
import my.tools.metricCollector.Metric

class GaugeTest extends TestKit(ActorSystem("MySystem"))
  with AnyFunSuiteLike
  with ImplicitSender
  with BeforeAndAfterAll
{
  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  class Gauge_test extends MetricMixinTest {
    val simpleGauge = metricCollector.Gauge.builder("SimpleGauge", "simple gauge", Map())
    val gaugeWithContext = metricCollector.Gauge.builder("GaugeContext", "gauge context", Map("ctx" -> "test"))
    val gaugeWithContexts = metricCollector.Gauge.builder("GaugeContexts", "gauge contexts", Map())
    override protected val collectors: Map[String,Metric] =
      simpleGauge.entry ++
      gaugeWithContext.entry ++
      gaugeWithContexts.entry
  }

  test("simple Gauge") {
    val gauge_test = new Gauge_test
    gauge_test.simpleGauge.collector ! metricCollector.Gauge.Set(20)
    val m = gauge_test.extractMetric(gauge_test.simpleGauge.name).head
    assert(m.name == gauge_test.simpleGauge.name)
    assert(m.value == 20.toDouble.toString)
  }

  test("gauge with context") {
    val gauge_test = new Gauge_test
    gauge_test.gaugeWithContext.collector ! metricCollector.Gauge.Set(10)
    val m = gauge_test.extractMetric(gauge_test.gaugeWithContext.name).head
    assert(m.name == gauge_test.gaugeWithContext.name)
    assert(m.value == 10.toDouble.toString)
    assert(m.labels("ctx") == "test")
  }

  test("gauge with contexts"){
    val gauge_test = new Gauge_test
    gauge_test.gaugeWithContexts.collector ! metricCollector.Gauge.Set(1,Map("1" -> "a", "res" -> 1.toDouble.toString))
    gauge_test.gaugeWithContexts.collector ! metricCollector.Gauge.Set(2,Map("1" -> "b", "res" -> 2.toDouble.toString))
    val m = gauge_test.extractMetric(gauge_test.gaugeWithContexts.name)
    assert(m.size == 3)
    m.foreach(item =>
      if (item.labels.nonEmpty)
        assert(item.labels("res") == item.value)
      else
        assert(item.value == 0.toDouble.toString)
    )
  }

}
