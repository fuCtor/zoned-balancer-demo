package com.twitter.finagle.loadbalancer

import com.twitter.finagle._
import com.twitter.finagle.loadbalancer.zoned.ZonedLeastLoaded
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.finagle.util.Rng
import com.twitter.util.{Activity, Future, Time}

object ZonedBalancers {

  val zoneField = "zone-name"

  private def newScopedBal[Req, Rep](label: String,
                                                   sr: StatsReceiver,
                                                   lbType: String,
                                                   bal: ServiceFactory[Req, Rep]
  ): ServiceFactory[Req, Rep] = {
    bal match {
      case balancer: Balancer[Req, Rep] => balancer.register(label)
      case _                            => ()
    }

    new ServiceFactoryProxy(bal) {
      val lbWithSuffix            = lbType + "_weighted"
      private[this] val typeGauge = sr.scope("algorithm").addGauge(lbWithSuffix)(1)

      override def close(when: Time): Future[Unit] = {
        typeGauge.remove()
        super.close(when)
      }
    }
  }
  def zoned(datacenterName: String, rng: Rng = Rng.threadLocal): LoadBalancerFactory = new LoadBalancerFactory {
    override def toString: String = "P2ZonedLeastLoaded"

    def newBalancer[Req, Rep](endpoints: Activity[IndexedSeq[EndpointFactory[Req, Rep]]],
                              exc: NoBrokersAvailableException,
                              params: Stack.Params
    ): ServiceFactory[Req, Rep] = {
      val sr        = params[param.Stats].statsReceiver
      val panicMode = params[PanicMode]
      val balancer  = new ZonedLeastLoaded(endpoints, panicMode, datacenterName, rng, sr, exc)
      newScopedBal(params[param.Label].label, sr, "p2c_zoned_least_loaded", balancer)
    }
  }
}
