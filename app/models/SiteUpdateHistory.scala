package models

import org.joda.time.DateTime
import play.api.Play._
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json.{Format, Json}
import util.MyPostgresDriver

import scala.concurrent.Future

case class SiteUpdateHistory(
                           id: Option[Long],
                           siteId: Long,
                           updateId: Long,
                           hash: String,
                           fullText: String,
                           time: DateTime
                         )

object SiteUpdateHistory {
  implicit val format: Format[SiteUpdateHistory] = Json.format[SiteUpdateHistory]

  protected val dbConfig = DatabaseConfigProvider.get[MyPostgresDriver](current)

  import dbConfig._
  import dbConfig.driver.api._

  class SiteUpdateHistoryTable(tag: Tag) extends Table[SiteUpdateHistory](tag, "site_update_history") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)

    def siteId = column[Long]("site_id")

    def updateId = column[Long]("update_id")

    def hash = column[String]("hash")

    def fullText = column[String]("full_text")

    def time = column[DateTime]("time")

    def site = foreignKey("site_siteupdatehistory", siteId, Site.table)(_.id)

    def update = foreignKey("updatehistory_siteupdatehistory", updateId, UpdateHistory.table)(_.id)

    def * = (id.?, siteId, updateId, hash, fullText, time) <> ((SiteUpdateHistory.apply _).tupled, SiteUpdateHistory.unapply)
  }

  val table = TableQuery[SiteUpdateHistoryTable]

  def getBySiteId(siteId: Long): Future[Seq[SiteUpdateHistory]] = {
    val query = table.filter(_.siteId === siteId).sortBy(_.time.desc).result

    db.run(query)
  }

  def create(newSiteUpdateHistory: SiteUpdateHistory): Future[SiteUpdateHistory] = {
    val insertion = (table returning table.map(_.id)) += newSiteUpdateHistory

    val insertedIDFuture = db.run(insertion)

    val createdCopy: Future[SiteUpdateHistory] = insertedIDFuture.map { resultID =>
      newSiteUpdateHistory.copy(id = Option(resultID))
    }

    createdCopy
  }
}
