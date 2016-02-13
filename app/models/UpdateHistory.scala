package models

import org.joda.time.DateTime
import play.api.Play._
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json.{Format, Json}
import util.MyPostgresDriver

import scala.concurrent.Future

case class UpdateHistory(
                          id: Option[Long],
                          runTime: DateTime,
                          successfulCount: Int,
                          updateCount: Int,
                          failedCount: Int
                        )

object UpdateHistory {
  implicit val format: Format[UpdateHistory] = Json.format[UpdateHistory]

  protected val dbConfig = DatabaseConfigProvider.get[MyPostgresDriver](current)

  import dbConfig._
  import dbConfig.driver.api._

  class UpdateHistoryTable(tag: Tag) extends Table[UpdateHistory](tag, "update_history") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)

    def runTime = column[DateTime]("run_time")

    def successfulCount = column[Int]("successful_count")

    def updateCount = column[Int]("update_count")

    def failedCount = column[Int]("failed_count")

    def * = (id.?, runTime, successfulCount, updateCount, failedCount) <>((UpdateHistory.apply _).tupled, UpdateHistory.unapply)
  }

  val table = TableQuery[UpdateHistoryTable]

  def getByID(updateHistoryID: Long): Future[Option[UpdateHistory]] = {
    val updateHistoryByID = table.filter { f =>
      f.id === updateHistoryID
    }.result.headOption

    db.run(updateHistoryByID)
  }
  
  def create(newUpdateHistory: UpdateHistory): Future[UpdateHistory] = {
    val insertion = (table returning table.map(_.id)) += newUpdateHistory

    val insertedIDFuture = db.run(insertion)

    val createdCopy: Future[UpdateHistory] = insertedIDFuture.map { resultID =>
      newUpdateHistory.copy(id = Option(resultID))
    }

    createdCopy
  }

  def updateCounts(updateId: Long, successfulCount: Int, updateCount: Int, failedCount: Int): Future[Option[UpdateHistory]] = {
    val query = for { u <- table if u.id === updateId } yield (u.successfulCount, u.updateCount, u.failedCount)
    val updateAction = query.update(successfulCount, updateCount, failedCount)

    db.run(updateAction).flatMap { _ =>
      getByID(updateId)
    }
  }
}
