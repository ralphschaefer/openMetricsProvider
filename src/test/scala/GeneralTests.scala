import org.scalatest.funsuite.AnyFunSuite

import scala.collection.mutable

class GeneralTests extends AnyFunSuite {

  test("test") {
    type context = Map[String, String]
    val m: mutable.Map[context,Int] = mutable.Map.empty[context,Int]
    val baseMap = Map("a"->"1","b"->"2")

    m(baseMap) = 1
    m(baseMap + ("a" -> "2")) = 2
    m(baseMap + ("c" -> "3")) = 3
    m(baseMap) += 1
    println(m)
    assert(true)
  }

}
