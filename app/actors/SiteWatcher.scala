package actors

import java.security.MessageDigest
import javax.inject.{Named, Inject}

import akka.actor.{ActorRef, ActorSystem, Actor}
import models.{UpdateHistory, Site, SiteUpdateHistory, UpdateErrorLog}
import org.joda.time.DateTime
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.ws._

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

object SiteWatcher {

  case class RunUpdate()

  trait UpdateResult

  case object Successful extends UpdateResult

  case object Updated extends UpdateResult

  case object Failed extends UpdateResult

}

class SiteWatcherScheduler @Inject() (val system: ActorSystem, @Named("site-watcher") val siteWatcherActor: ActorRef)
{
  system.scheduler.schedule(0.microseconds, 5.minutes, siteWatcherActor, SiteWatcher.RunUpdate )
}

class SiteWatcher @Inject()(ws: WSClient) extends Actor {

  import SiteWatcher._

  def updateSite(site: Site, updateId: Long): Future[UpdateResult] = {
    val request: WSRequest = ws.url(site.url).withRequestTimeout(5000)
    val responseFuture: Future[WSResponse] = request.get

    responseFuture.recover {
      case e: Exception =>
        val updateErrorLog = UpdateErrorLog(None, site.id.get, updateId,
          s"Site unavailable, exception ${e.getMessage()}", DateTime.now())
        UpdateErrorLog.create(updateErrorLog).map { _ => Failed }
    }.flatMap { case response: WSResponse =>
      if (response.status == 200) {
        val digest = MessageDigest.getInstance("MD5")
        val md5hash = digest.digest(response.bodyAsBytes).map("%02x".format(_)).mkString
        val oldHash = site.lastHash.getOrElse("_none_")

        if (md5hash == oldHash) {
          Future.successful(Successful)
        }
        else {
          val responseBodyFixed = response.header("Content-Type").filter(_.toLowerCase.contains("charset")).fold(new String(response.body.getBytes("ISO-8859-1") , "UTF-8"))(_ => response.body)
          val siteUpdateHistory = SiteUpdateHistory(None, site.id.get, updateId, md5hash, responseBodyFixed, DateTime.now())
          val createSiteUpdateHistoryFuture = SiteUpdateHistory.create(siteUpdateHistory)
          val updateHashFuture = Site.updateHash(site.id.get, md5hash)
          (for {
            suh <- createSiteUpdateHistoryFuture
            s <- updateHashFuture
          } yield (suh, s)).map { _ => Updated }
        }
      }
      else {
        val updateErrorLog = UpdateErrorLog(None, site.id.get, updateId,
          s"Site unavailable, response status ${response.status}", DateTime.now())
        UpdateErrorLog.create(updateErrorLog).map { _ => Failed }
      }
    }
  }

  def runUpdate = {
    val updateHistory = UpdateHistory(None, DateTime.now(), 0, 0, 0)
    val updateHistoryFuture = UpdateHistory.create(updateHistory)
    val getSitesFuture = Site.list
    val combinedFuture = for {
      uh <- updateHistoryFuture
      s <- getSitesFuture
    } yield (uh, s)
    combinedFuture.map { case (uh: UpdateHistory, sites: Seq[Site]) =>
      val resultsFuture = sites.map { site =>
        updateSite(site, uh.id.get)
      }
        Future.sequence(resultsFuture).map { resultsSeq =>
          val resultsMap = resultsSeq.groupBy(identity).mapValues(_.size)
          UpdateHistory.updateCounts(uh.id.get, resultsMap.getOrElse(Successful, 0), resultsMap.getOrElse(Updated, 0), resultsMap.getOrElse(Failed, 0))
        }
    }
  }

  def receive = {
    case RunUpdate => runUpdate
  }
}