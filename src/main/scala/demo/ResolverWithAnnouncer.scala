package demo

import com.twitter.finagle.loadbalancer.ZonedBalancers
import com.twitter.finagle.{Addr, Address, Announcement, Announcer, Resolver}
import com.twitter.util.{Future, Var}

import java.net.InetSocketAddress
import java.util.concurrent.{ConcurrentHashMap, ConcurrentMap}
import scala.jdk.CollectionConverters._

class ResolverWithAnnouncer extends Resolver with Announcer {
  private val services: ConcurrentMap[String, Address] = new ConcurrentHashMap()
  override val scheme: String                          = "simple"

  override def bind(arg: String): Var[Addr] =
    if (arg == "all") {
      val addrs = services
        .values()
        .asScala
        .toList
        .toSet

      Var.value(Addr.Bound(addrs))
    } else
      Option(services.get(arg)) match {
        case Some(address) => Var.value(Addr.Bound(address))
        case None          => Var.value(Addr.Neg)
      }

  override def announce(addr: InetSocketAddress, name: String): Future[Announcement] = Future {
    val Array(svcName, zone) = name.split('/')
    println(s"Register: $svcName => $addr")
    services.put(svcName, Address.Inet(addr, Addr.Metadata(ZonedBalancers.zoneField -> zone)))

    () =>
      Future {
        services.remove(svcName)
      }
  }
}
