package com.twitter.finagle.loadbalancer.zoned

import com.twitter.finagle.Status
import com.twitter.finagle.loadbalancer.DistributorT
import com.twitter.finagle.loadbalancer.p2c.P2CPick
import com.twitter.finagle.stats.{StatsReceiver, Verbosity}
import com.twitter.finagle.util.Rng

private[loadbalancer] class ZonedDist[Req, Rep, Node <: ZonedNode[Req, Rep]](vector: Vector[Node],
                                                                             currentZone: String,
                                                                             failingNode: Node,
                                                                             rng: Rng,
                                                                             statsReceiver: StatsReceiver,
                                                                             maxLoad: Option[Double]
) extends DistributorT[Node](vector) {
  override type This = ZonedDist[Req, Rep, Node]

  // There is nothing to rebuild (we don't partition in P2C) so we just return
  // `this` instance.

  override def rebuild(vec: Vector[Node]): This = new ZonedDist(vec, currentZone, failingNode, rng, statsReceiver, maxLoad)

  val (inSameDatacenter, inOtherDatacenter) = vector.partition { node =>
    node.zone.contains(currentZone)
  }

  private def isAcceptable(node: Node): Boolean =  node.isAvailable && maxLoad.forall(node.load <= _)

  private[this] val p2cZeroCounter = statsReceiver.counter(
    description = "counts the number of times p2c selects two nodes with a zero load",
    Verbosity.ShortLived,
    "p2c_zoned",
    "zero")

  override def pick(): Node =
    if (vector.isEmpty) failingNode
    else {
      val other = inOtherDatacenter.filter(isAcceptable)
      val same  = inSameDatacenter.filter(isAcceptable)
      val subVector = if (inSameDatacenter.isEmpty && inOtherDatacenter.isEmpty) {
        vector
      } else {
        if (same.nonEmpty) same else other
      }
      P2CPick.pick(subVector, subVector.size, rng, p2cZeroCounter)
    }

  override def needsRebuild: Boolean = false

  override def rebuild(): ZonedDist[Req, Rep, Node] = this
}
