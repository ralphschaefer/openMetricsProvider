package my.tools

import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatest.BeforeAndAfterAll
import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender,TestKit}
import my.tools.metricCollector.Metric

class CountTest
  extends TestKit(ActorSystem("MySystem"))
  with AnyFunSuiteLike
  with ImplicitSender
  with BeforeAndAfterAll
{
  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  class Counter_test extends MetricMixinTest {
    val simpleCounter = metricCollector.Count.builder("SimpleCount", "simple count", Map())
    val counterWithContext = metricCollector.Count.builder("CountContext", "count context", Map("ctx" -> "test"))
    val counterWithContexts = metricCollector.Count.builder("CountContexts", "count contexts", Map())
    override protected val collectors: Map[String,Metric] =
      simpleCounter.entry ++
      counterWithContext.entry ++
      counterWithContexts.entry
  }

  test("simple counter") {
    val counter_test = new Counter_test
    counter_test.simpleCounter.collector ! metricCollector.Count.Inc
    val m = counter_test.extractMetric(counter_test.simpleCounter.name).head
    assert(m.name == counter_test.simpleCounter.name)
    assert(m.value == 1.toDouble.toString)
  }

  test("counter with context") {
    val counter_test = new Counter_test
    counter_test.counterWithContext.collector ! metricCollector.Count.Inc
    counter_test.counterWithContext.collector ! metricCollector.Count.Inc
    val m = counter_test.extractMetric(counter_test.counterWithContext.name).head
    assert(m.name == counter_test.counterWithContext.name)
    assert(m.value == 2.toDouble.toString)
    assert(m.labels("ctx") == "test")
  }

  test("counter with contexts"){
    val counter_test = new Counter_test
    counter_test.counterWithContexts.collector ! metricCollector.Count.Inc(Map("1" -> "a", "res" -> 1.toDouble.toString))
    counter_test.counterWithContexts.collector ! metricCollector.Count.Inc(Map("1" -> "b", "res" -> 2.toDouble.toString))
    counter_test.counterWithContexts.collector ! metricCollector.Count.Inc(Map("1" -> "b", "res" -> 2.toDouble.toString))
    val m = counter_test.extractMetric(counter_test.counterWithContexts.name)
    assert(m.size == 3)
    m.foreach(item =>
      if (item.labels.nonEmpty)
        assert(item.labels("res") == item.value)
      else
        assert(item.value == 0.toDouble.toString)
    )
  }

}
