package com.twitter.finagle.loadbalancer.locality

import com.twitter.finagle.loadbalancer.Balancer
import com.twitter.finagle.util.Rng

private[loadbalancer] trait Zoned[Req, Rep] extends Balancer[Req, Rep] { self =>
  protected def rng: Rng

  protected override type Node <: ZonedNode[Req, Rep]
  protected override type Distributor = ZonedDist[Req, Rep, Node]

  protected def currentZone: String

  def additionalMetadata: Map[String, Any] = Map("current_zone" -> currentZone)

  protected def initDistributor(): Distributor = new ZonedDist(Vector.empty, currentZone, failingNode, rng, statsReceiver, Some(1))
}
