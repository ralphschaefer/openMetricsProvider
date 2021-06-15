package my.tools

import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatest.BeforeAndAfterAll
import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender,TestKit}
import my.tools.metricCollector.Metric

class BucketsTest
  extends TestKit(ActorSystem("MySystem"))
  with AnyFunSuiteLike
  with ImplicitSender
  with BeforeAndAfterAll
{
  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  val definedBuckets=Set(0.01, 0.05, 0.1, 0.5, 1, 5, 10)

  class Buckets_test extends MetricMixinTest {
    val simpleBucket = metricCollector.Buckets.builder("SimpleBucket", "simple bucket", Map(), definedBuckets)
    val bucketWithContext = metricCollector.Buckets.builder("BucketContext", "bucket context", Map("ctx" -> "test"), definedBuckets)
    val bucketWithContexts = metricCollector.Buckets.builder("BucketContexts", "bucket context", Map(), definedBuckets)
    override protected val collectors: Map[String,Metric] =
      simpleBucket.entry ++
      bucketWithContext.entry ++
      bucketWithContexts.entry
  }

  test("simple Bucket") {
    val buckets_test = new Buckets_test
    buckets_test.simpleBucket.collector ! metricCollector.Buckets.Item(4.5)
    buckets_test.simpleBucket.collector ! metricCollector.Buckets.Item(0.2)
    val m = buckets_test.extractMetric(buckets_test.simpleBucket.name)
    val expected = Map(
      "+Inf" -> "2.0",
      "10.0" -> "2.0",
      "5.0" -> "2.0",
      "1.0" -> "1.0",
      "0.5" -> "1.0",
      "0.1" -> "0.0",
      "0.05" -> "0.0",
      "0.01" -> "0.0"
    )
    for (item <- m) {
      if (item.name.contains("sum")) {
        assert(item.value == 4.7.toString)
      }
      if (item.name.contains("count")) {
        assert(item.value == 2.0.toString)
      }
      if (item.name.contains("bucket")) {
        assert( expected(item.labels("le")) == item.value)
      }
    }
  }

  test("Bucket context") {
    val buckets_test = new Buckets_test
    buckets_test.bucketWithContext.collector ! metricCollector.Buckets.Item(4.5)
    buckets_test.bucketWithContext.collector ! metricCollector.Buckets.Item(0.2)
    val m = buckets_test.extractMetric(buckets_test.bucketWithContext.name)
    val expected = Map(
      "+Inf" -> "2.0",
      "10.0" -> "2.0",
      "5.0" -> "2.0",
      "1.0" -> "1.0",
      "0.5" -> "1.0",
      "0.1" -> "0.0",
      "0.05" -> "0.0",
      "0.01" -> "0.0"
    )
    for (item <- m) {
      assert(item.labels("ctx") == "test")
      if (item.name.contains("sum")) {
        assert(item.value == 4.7.toString)
      }
      if (item.name.contains("count")) {
        assert(item.value == 2.0.toString)
      }
      if (item.name.contains("bucket")) {
        assert( expected(item.labels("le")) == item.value)
      }
    }
  }

  test("Bucket contexts") {
    val buckets_test = new Buckets_test
    buckets_test.bucketWithContexts.collector ! metricCollector.Buckets.Item(4.5, Map("bucket" -> "1"))
    buckets_test.bucketWithContexts.collector ! metricCollector.Buckets.Item(0.2, Map("bucket" -> "1"))
    buckets_test.bucketWithContexts.collector ! metricCollector.Buckets.Item(5.5, Map("bucket" -> "2"))
    buckets_test.bucketWithContexts.collector ! metricCollector.Buckets.Item(0.2, Map("bucket" -> "2"))
    val m = buckets_test.extractMetric(buckets_test.bucketWithContexts.name)
    val expected1 = Map(
      "+Inf" -> "2.0",
      "10.0" -> "2.0",
      "5.0" -> "2.0",
      "1.0" -> "1.0",
      "0.5" -> "1.0",
      "0.1" -> "0.0",
      "0.05" -> "0.0",
      "0.01" -> "0.0"
    )
    val expected2 = Map(
      "+Inf" -> "2.0",
      "10.0" -> "2.0",
      "5.0" -> "1.0",
      "1.0" -> "1.0",
      "0.5" -> "1.0",
      "0.1" -> "0.0",
      "0.05" -> "0.0",
      "0.01" -> "0.0"
    )
    for (item <- m) {
      if (item.labels.get("bucket").isEmpty)
        assert(item.value == 0.0.toString)
      else {
        if (item.labels("bucket") == "1") {
          if (item.name.contains("sum")) {
            assert(item.value == 4.7.toString)
          }
          if (item.name.contains("count")) {
            assert(item.value == 2.0.toString)
          }
          if (item.name.contains("bucket")) {
            assert(expected1(item.labels("le")) == item.value)
          }
        }
        if (item.labels("bucket") == "2") {
          if (item.name.contains("sum")) {
            assert(item.value == 5.7.toString)
          }
          if (item.name.contains("count")) {
            assert(item.value == 2.0.toString)
          }
          if (item.name.contains("bucket")) {
            assert(expected2(item.labels("le")) == item.value)
          }
        }
      }
    }
  }

}
