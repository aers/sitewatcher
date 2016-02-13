package util

import com.github.tminglei.slickpg._

trait MyPostgresDriver extends ExPostgresDriver
  with PgEnumSupport with PgDateSupportJoda {

  override val api = MyAPI

  object MyAPI extends API with DateTimeImplicits

}

object MyPostgresDriver extends MyPostgresDriver