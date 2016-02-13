package models

import org.joda.time.DateTime
import play.api.Play._
import play.api.data._
import play.api.data.Forms._
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json.{Format, Json}
import util.MyPostgresDriver

import scala.concurrent.Future

case class Site(
                 id: Option[Long],
                 alias: String,
                 url: String,
                 lastUpdated: Option[DateTime],
                 lastHash: Option[String]
               )

object Site {
  implicit val format: Format[Site] = Json.format[Site]

  val FormMapping = Form(
    mapping(
      "id" -> optional(longNumber),
      "alias" -> nonEmptyText,
      "url" -> nonEmptyText,
      "lastUpdated" -> optional(jodaDate),
      "lastHash" -> optional(text)
    )(Site.apply)(Site.unapply)
  )

  protected val dbConfig = DatabaseConfigProvider.get[MyPostgresDriver](current)

  import dbConfig._
  import dbConfig.driver.api._

  class SitesTable(tag: Tag) extends Table[Site](tag, "sites") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)

    def alias = column[String]("alias")

    def url = column[String]("url")

    def lastUpdated = column[DateTime]("last_updated")

    def lastHash = column[String]("last_hash")

    def * = (id.?, alias, url, lastUpdated.?, lastHash.?) <>((Site.apply _).tupled, Site.unapply)
  }

  val table = TableQuery[SitesTable]

  def list: Future[Seq[Site]] = {
    val siteList = table.result
    db.run(siteList)
  }

  def getByID(siteID: Long): Future[Option[Site]] = {
    val siteByID = table.filter { f =>
      f.id === siteID
    }.result.headOption

    db.run(siteByID)
  }

  def getByAlias(siteAlias: String): Future[Option[Site]] = {
    val siteByAlias = table.filter { f =>
      f.alias === siteAlias
    }.result.headOption

    db.run(siteByAlias)
  }

  def create(newSite: Site): Future[Site] = {
    val insertion = (table returning table.map(_.id)) += newSite

    val insertedIDFuture = db.run(insertion)

    val createdCopy: Future[Site] = insertedIDFuture.map { resultID =>
      newSite.copy(id = Option(resultID))
    }

    createdCopy
  }

  def updateHash(siteID: Long, hash: String): Future[Option[Site]] = {
    val query = for { s <- table if s.id === siteID } yield (s.lastHash, s.lastUpdated)
    val updateAction = query.update(hash, DateTime.now())

    db.run(updateAction).flatMap { _ =>
      getByID(siteID)
    }
  }

  def getByUrl(url: String): Future[Option[Site]] = {
    val siteByUrl = table.filter { f =>
      f.url === url
    }.result.headOption

    db.run(siteByUrl)
  }
}
