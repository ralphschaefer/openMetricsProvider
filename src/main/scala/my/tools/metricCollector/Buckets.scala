package my.tools.metricCollector

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.scalalogging.StrictLogging
import my.tools.metricCollector.Metric.context
import scala.collection.mutable
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.DurationInt

class Buckets(metricContext: Metric.context, bucketSet: Set[Double])
  extends MetricCollector(metricContext) {

  import Buckets._

  private val buckets: mutable.Map[Metric.context, Bucket] = mutable.Map.empty[context, Bucket]

  private def updateBucket(value: Double, ctx: Metric.context) =
    if (buckets.contains(ctx))
        buckets(ctx)(value)
      else {
        val m = new Bucket(bucketSet)
        m(value)
        buckets(ctx) = m
      }

  override def receive: Receive = {
    case Init =>
      buckets(metricContext) = new Bucket(bucketSet)
    case Item(value, ctx) if ctx.nonEmpty =>
      updateBucket(value, metricContext ++ ctx)
    case Item(value,_) =>
      updateBucket(value, metricContext)

    case MetricCollector.Query =>
      sender() ! QueryResults(buckets.toMap)
  }

}

object Buckets {

  def builder(
               name:String, description:String,
               metricContext: Metric.context,
               bucketSet: Set[Double]
             )(implicit system: ActorSystem):Metric = {
    import system.dispatcher
    val collectorActor =
      system.actorOf(Props(new Buckets(metricContext, bucketSet)), name+"_bucket_collector_" + java.util.UUID.randomUUID.toString)
    val b = new BucketMetric(collectorActor, name, description)
    b.collector ! Init
    b
  }

  sealed trait Probe
  private case object Init extends Probe
  case class Item(value: Double, metricContext: Metric.context=Map()) extends Probe
  case class QueryResults(buckets: Map[Metric.context, Bucket])

  class BucketMetric(bucketActor: ActorRef, name:String, description:String)(implicit executor:ExecutionContext)
    extends Metric(bucketActor, name, description)
    with StrictLogging {
    override def metricType = "histogram"
    implicit val timeout = Timeout(500.millis)

    def metric: String = {
      val f = (bucketActor ? MetricCollector.Query).map{
        case QueryResults(buckets) =>
          Some(
            (for((k,v) <- buckets) yield v.out(name,k)).flatten.mkString("\n")
          )
        case other =>
          logger.error(s"unexpected: $other")
          None
      }.fallbackTo(Future(None)).filter(_.nonEmpty).map(_.get)
      Await.result(f, timeout.duration)
    }
  }

  class Bucket(bucketInit: Set[Double]) {
    case class BucketItem(le:Double, count:Long) {
      def inc = BucketItem(le,count+1)
    }
    private val buckets: Array[BucketItem] =
      (for (le <- bucketInit.toSeq.sorted) yield BucketItem(le,0)).toArray
    private var sum = 0.0
    private var count = 0
    def apply(probe:Double) = {
      count += 1
      sum += probe
      for (i <- buckets.indices)
        if (probe <= buckets(i).le) buckets(i)=buckets(i).inc
    }
    def out(name:String, ctx: Metric.context):Seq[String] = {
      buckets.toSeq.map(bucket => s"${name}_bucket${Metric.writeContext(ctx ++ Map("le" -> bucket.le.toString))} ${bucket.count.toDouble}") ++
      Seq(s"${name}_bucket${Metric.writeContext(ctx ++ Map("le" -> "+Inf"))} ${count.toDouble}") ++
      Seq(s"${name}_sum${Metric.writeContext(ctx)} ${sum}") ++
      Seq(s"${name}_count${Metric.writeContext(ctx)} ${count.toDouble}")
    }
  }

}