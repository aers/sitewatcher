package controllers

import com.sksamuel.diffpatch.DiffMatchPatch
import models.{Site, SiteUpdateHistory}
import play.api.libs.concurrent.Execution.Implicits._
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import play.api.mvc._
import util.MyDateTimeFormat

import scala.concurrent.Future

object Application extends Controller {

  def listSites = Action.async { implicit request =>
    val siteFuture: Future[Seq[Site]] = Site.list

    val response = siteFuture.map { sites =>
      Ok(views.html.sitelist(sites))
    }

    response
  }

  def getFeed(alias: String) = Action.async { request =>
    val getSiteFuture = Site.getByAlias(alias)

    getSiteFuture.flatMap {
      case None => {
        Future.successful(Ok(
          <rss version="2.0">
            <channel>
              <title>No site found with alias {alias}</title>
              <description>No site found with alias {alias}</description>
              <link>#</link>
            </channel>
          </rss>).as("application/rss+xml"))
      }
      case Some(site) => {
        val getSiteUpdateHistoryFuture: Future[Seq[SiteUpdateHistory]] = SiteUpdateHistory.getBySiteId(site.id.get)
        getSiteUpdateHistoryFuture.map { siteUpdateHistorySeq =>
          Ok(
            <rss version="2.0">
              <channel>
                <title>sitewatcher RSS - {alias} : {site.url}</title>
                <description>Watches for changes to the page {site.url}</description>
                <link>{site.url}</link>
                {siteUpdateHistorySeq.map { siteUpdateHistory =>
                <item>
                  <title>{alias} : Site changed!</title>
                  <link>{site.url}</link>
                  <description>Website {site.url} changed at {MyDateTimeFormat.OUTPUT.print(siteUpdateHistory.time)}</description>
                </item>
              }}
              </channel>
            </rss>
          ).as("application/rss+xml")
        }
      }
    }
  }

  def latestDiff(alias: String) = Action.async { request =>
    val getSiteFuture = Site.getByAlias(alias)

    getSiteFuture.flatMap {
      case None => {
        Future.successful(NotFound(views.html.notfound(s"Site with alias $alias not found")))
      }
      case Some(site) => {
        val getSiteUpdateHistoryFuture: Future[Seq[SiteUpdateHistory]] = SiteUpdateHistory.getBySiteId(site.id.get)
        getSiteUpdateHistoryFuture.map { siteUpdateHistorySeq: Seq[SiteUpdateHistory] =>
          if (siteUpdateHistorySeq.size < 2)
            NotFound(views.html.notfound(s"Site with alias $alias doesn't have enough entries to diff"))
          else
            {
              val diffMaker = new DiffMatchPatch()
              val diffs = diffMaker.diff_main(siteUpdateHistorySeq.lift(1).get.fullText, siteUpdateHistorySeq.lift(0).get.fullText)
              val diffHtml = diffMaker.diff_prettyHtml(diffs).replaceAll("&para;", "")
              Ok(views.html.diff(site, diffHtml))
            }
        }
      }
    }
  }

  def addSiteForm = Action { implicit request =>
    Ok(views.html.siteform(Site.FormMapping))
  }

  def addSiteFormSubmit = Action.async { implicit request =>
    Site.FormMapping.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(BadRequest(views.html.siteform(formWithErrors)))
      },
      site => {
        val getByUrlFuture = Site.getByUrl(site.url)
        val getByAliasFuture = Site.getByAlias(site.alias)

        val combinedFuture = for {
          byUrl <- getByUrlFuture
          byAlias <- getByAliasFuture
        } yield (byUrl, byAlias)

        combinedFuture.flatMap {
          case (Some(site), None) => {
            val responseMessage = s"This site already exists in the watcher with alias ${site.alias}"

            Future.successful(Redirect(routes.Application.addSiteForm()).flashing("error" -> responseMessage))
          }
          case (None, Some(site)) => {
            val responseMessage = s"This alias already exists in the watcher with URL ${site.url}"

            Future.successful(Redirect(routes.Application.addSiteForm()).flashing("error" -> responseMessage))
          }
          case (Some(site1), Some(site2)) => {
            val responseMessage = s"This site already exists in the watcher with alias ${site1.alias} and this alias already exists in the watcher with URL ${site2.url}"

            Future.successful(Redirect(routes.Application.addSiteForm()).flashing("error" -> responseMessage))
          }
          case (None, None) => {
            val createdSiteFuture: Future[Site] = Site.create(site)

            createdSiteFuture.map { createdSite => Redirect(routes.Application.listSites()).flashing("message" -> s"Site ${site.alias} successfully added") }
          }
        }
    })
  }
}