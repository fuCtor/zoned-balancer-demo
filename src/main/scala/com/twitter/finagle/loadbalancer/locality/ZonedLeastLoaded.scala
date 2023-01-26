package com.twitter.finagle.loadbalancer.locality

import com.twitter.finagle.loadbalancer._
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.finagle.util.Rng
import com.twitter.finagle.{Address, NoBrokersAvailableException, ServiceFactoryProxy}
import com.twitter.util.Activity

private[loadbalancer] final class ZonedLeastLoaded[Req, Rep](
  protected val endpoints: Activity[IndexedSeq[EndpointFactory[Req, Rep]]],
  private[loadbalancer] val panicMode: PanicMode,
  protected val currentZone: String,
  protected val rng: Rng,
  protected val statsReceiver: StatsReceiver,
  protected val emptyException: NoBrokersAvailableException
) extends Zoned[Req, Rep] with LeastLoaded[Req, Rep] with Updating[Req, Rep] {

  case class Node(factory: EndpointFactory[Req, Rep], zone: Option[String])
    extends ServiceFactoryProxy[Req, Rep](factory) with LeastLoadedNode with ZonedNode[Req, Rep]

  protected def newNode(factory: EndpointFactory[Req, Rep]): Node = {
    val zone = (factory.address match {
      case Address.Inet(_, metadata)           => metadata.get(ZonedBalancers.zoneField)
      case Address.ServiceFactory(_, metadata) => metadata.get(ZonedBalancers.zoneField)
      case Address.Failed(_)                   => None
    }).collect { case name: String =>
      name
    }


    Node(factory, zone)
  }
}
