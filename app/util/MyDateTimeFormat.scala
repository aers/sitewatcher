package util

import org.joda.time.format.DateTimeFormatter
import org.joda.time.format.DateTimeFormat

object MyDateTimeFormat {
  val OUTPUT: DateTimeFormatter = DateTimeFormat.forPattern("MM/dd/yyyy HH:mm:ss")
}
