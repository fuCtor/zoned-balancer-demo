package demo

import com.twitter.app.LoadService.Binding
import com.twitter.conversions.DurationOps.richDurationFromInt
import com.twitter.finagle.{Announcer, Resolver, Service}
import com.twitter.finagle.http.{Request, Response, Status}
import com.twitter.finagle.loadbalancer.ZonedBalancers
import com.twitter.finagle.util.{DefaultTimer, Rng}
import com.twitter.util.{Await, Duration, Future}

import scala.util.Random

object Main extends com.twitter.app.App {
  val simpleResolver = new ResolverWithAnnouncer

  protected[this] override val loadServiceBindings: Seq[Binding[_]] =
    Seq(new Binding(classOf[Resolver], Seq(simpleResolver)), new Binding(classOf[Announcer], Seq(simpleResolver)))

  import DefaultTimer.Implicit

  val serverSize   = 20
  val requestCount = 20

  val counter         = Counter(serverSize)
  val report          = Report(counter)
  val random          = new Random(42)
  val currentRng: Rng = Rng(random)

  def serviceBuilder(step: Duration)(idx: Int): Service[Request, Response] =
    Service.mk { _ =>
      Future.Unit.delayed(10.millis + step * idx).map { _ =>
        if (random.nextInt(100) < 50) {
          Response(Status.NoContent)
        } else {
          Response(Status.ServiceUnavailable)
        }
      }
    }

  val serverBuilder = ServerBuilder.build(counter, serviceBuilder(100.millis))

  def main() = {
    val servers = (0 until serverSize).map(serverBuilder)
    val runner  = TestRunner(report, servers, requestCount * serverSize)

    val balancers = Seq(
      "P2CZonedLeastLoaded[DC1]" -> ZonedBalancers.zoned("dc1", rng = currentRng),
      "P2CZonedLeastLoaded[DC2]" -> ZonedBalancers.zoned("dc2", rng = currentRng)
    )

    val f = for {
      _ <- Future.traverseSequentially(balancers)((runner.doTest(1) _).tupled)
      _ <- runner.stop
    } yield report.printSummary()

    Await.result(f)
  }

}
