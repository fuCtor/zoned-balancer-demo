package demo

import com.twitter.util.Future
import de.vandermeer.asciitable.{AsciiTable, CWC_LongestWord}

import scala.collection.mutable.ListBuffer

class Report(counter: Counter) {

  private val turns = ListBuffer.empty[Report.Row]

  def turn[A](id: Int, title: String)(f: => Future[A]): Future[A] = {
    println(s"Start: $title")
    counter.reset()
    val started = System.nanoTime()
    f.ensure {
      val elapsed = (System.nanoTime() - started).toDouble / 1000000000
      turns += Report.Row(
        id,
        title    = title,
        elapsed  = elapsed,
        counters = counter.result.toList.sortBy(_._1).map(_._2),
        total    = counter.total
      )
    }
  }

  def printSummary(): Unit = {
    val table = new AsciiTable()
    table.addRule()
    table.addRow(
      "" :: "Name" :: "Elapsed" :: "Total" :: (0 until counter.result.size).toList.map(i => s"$i/${Util.dcName(i)}"): _*
    )
    table.addRule()
    turns.result().sortBy(r => (r.id, r.elapsed)).foreach { row =>
      val percents = row.counters.map(v => Report.round(v.toDouble * 100 / row.total, 2))
      table.addRow(row.id :: row.title :: Report.round(row.elapsed, 3) :: row.total :: percents: _*)
      table.addRule()
    }
    table.getContext.setWidth(200)
    table.getRenderer.setCWC(new CWC_LongestWord)
    println(table.render())
  }

}

object Report {
  def round(value: Double, n: Int): Double = {
    val k = Math.pow(10, n).toLong
    (value * k).round.toDouble / k
  }
  case class Row(id: Int, title: String, elapsed: Double, counters: List[Int], total: Int)

  def apply(counter: Counter): Report = new Report(counter)
}
