package models

import org.joda.time.DateTime
import play.api.Play._
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json.{Format, Json}
import util.MyPostgresDriver

import scala.concurrent.Future

case class UpdateErrorLog(
                          id: Option[Long],
                          siteId: Long,
                          updateId: Long,
                          errorReason: String,
                          time: DateTime
                        )

object UpdateErrorLog {
  implicit val format: Format[UpdateErrorLog] = Json.format[UpdateErrorLog]

  protected val dbConfig = DatabaseConfigProvider.get[MyPostgresDriver](current)

  import dbConfig._
  import dbConfig.driver.api._

  class UpdateErrorLogTable(tag: Tag) extends Table[UpdateErrorLog](tag, "update_error_log") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)

    def siteId = column[Long]("site_id")

    def updateId = column[Long]("update_id")

    def errorReason = column[String]("error_reason")

    def time = column[DateTime]("time")

    def site = foreignKey("site_updateerrorlog", siteId, Site.table)(_.id)

    def update = foreignKey("updatehistory_updateerrorlog", updateId, UpdateHistory.table)(_.id)

    def * = (id.?, siteId, updateId, errorReason, time) <> ((UpdateErrorLog.apply _).tupled, UpdateErrorLog.unapply)
  }

  val table = TableQuery[UpdateErrorLogTable]

  def create(newUpdateErrorLog: UpdateErrorLog): Future[UpdateErrorLog] = {
    val insertion = (table returning table.map(_.id)) += newUpdateErrorLog

    val insertedIDFuture = db.run(insertion)

    val createdCopy: Future[UpdateErrorLog] = insertedIDFuture.map { resultID =>
      newUpdateErrorLog.copy(id = Option(resultID))
    }

    createdCopy
  }
}
