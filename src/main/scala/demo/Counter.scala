package demo

import demo.Main.counter

import java.util.concurrent.atomic.AtomicInteger

case class Counter(size: Int) {

  private val counters = (0 until size).map({ idx =>
    (idx, new AtomicInteger(0))
  }).toMap

  def count(idx: Int): Unit = counters.get(idx).foreach(_.incrementAndGet())

  def reset(): Unit = counters.foreach(_._2.set(0))

  def result: Map[Int, Int] = counters.map {
    case (k, v) => k -> v.get()
  }

  def printSummary(title: String): Unit = {
    println(title)
    counters.toList.sortBy(t => t._1).foreach {
      case (idx, counter) => println(s"[$idx] ${counter.get()}")
    }

    println(s"Affected subset size: ${affected.size}")
    println(s"Affected subset: ${affected}")
    println(s"Requests total: ${counter.total}")
  }


  def affected: List[Int] = counters.filter(_._2.get() > 0).keys.toList.sorted
  def total = counters.map(_._2.get()).sum
}
