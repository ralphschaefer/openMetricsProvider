package my.tools

import my.tools.metric.MetricMixin

trait MetricMixinTest extends MetricMixin {
  def extractMetric(name:String) = {
    collectors(name)().split("\n").toSeq.drop(2).map(line =>
       new MetricMixinTest.extractedMetric(line.split(" "))
    )
  }
}

object MetricMixinTest {
  private def splitHeader(n: String):(String,Map[String,String]) = {
    val nn: Array[String] = n.split("\\{")
    if (nn.size == 1)
      (n,Map())
    else {
      val labels = nn(1).split(",").toVector
      val h: Map[String, String] = labels.filter(_.length>2).map{ label =>
        val hh: Array[String] = label.split("=")
        hh(0) -> hh(1).replaceAll("\"","")
      }.toMap
      (nn(0),h)
    }
  }
  case class extractedMetric(name:String, value:String, labels:Map[String, String]) {
    def this(m:Array[String]) = this(
      splitHeader(m(0))._1,
      m(1),
      splitHeader(m(0))._2
    )
  }

}
