package com.twitter.finagle.loadbalancer.zoned

import com.twitter.finagle.loadbalancer.NodeT

private[loadbalancer] trait ZonedNode[Req, Rep] extends NodeT[Req, Rep] {
  def zone: Option[String]
}
