package demo

import com.twitter.finagle.http.service.HttpResponseClassifier
import com.twitter.finagle.http.{Request, Response}
import com.twitter.finagle.{Http, ListeningServer, Service}
import com.twitter.finagle.loadbalancer.LoadBalancerFactory
import com.twitter.util.{Future, NullMonitor}

import java.net.InetSocketAddress
import java.util.concurrent.atomic.AtomicInteger

class TestRunner(report: Report, servers: Seq[ListeningServer], requestCount: Int) {
  private val dest = servers
    .flatMap(_.boundAddress match {
      case address: InetSocketAddress => Some(s"localhost:${address.getPort}")
      case _                          => None
    })
    .mkString(",")

  def stop: Future[Unit] = servers.foldLeft(Future.Unit) { case (f, s) =>
    f.flatMap(_ => s.close())
  }

  def doTest(clientCount: Int)(title: String, balancer: LoadBalancerFactory): Future[Unit] = {
    balancer.toString

    def buildClient: Service[Request, Response] =
      Http.client
        .withResponseClassifier(HttpResponseClassifier.ServerErrorsAsFailures)
        .withLoadBalancer(balancer)
        .withMonitor(NullMonitor)
        .newService("simple!all")

    val idx = new AtomicInteger(requestCount)

    def run(client: Service[Request, Response]): Future[Unit] = if (idx.getAndDecrement() > 0)
      client(Request("/")).flatMap(_ => run(client)).transform(_ => Future.Unit)
    else Future.Unit

    val currentClient = buildClient
    report
      .turn(clientCount, title)(Future.collect(Future.parallel(clientCount) {
        Future.collect(Future.parallel(servers.size)(run(currentClient)))
      }))
      .unit
  }
}

object TestRunner {
  def apply(report: Report, servers: Seq[ListeningServer], requestCount: Int): TestRunner =
    new TestRunner(report, servers, requestCount)
}
