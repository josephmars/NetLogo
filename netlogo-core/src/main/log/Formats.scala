// (C) Uri Wilensky. https://github.com/NetLogo/NetLogo

package org.nlogo.log

import java.time.format.DateTimeFormatter
import java.lang.{
  Boolean => BoxedBoolean
, Double  => BoxedDouble
, Integer => BoxedInt
, Long    => BoxedLong
}

import scala.collection.JavaConverters._

import org.nlogo.core.LogoList

object DateTimeFormats {
  private[log] val file     = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss.SSS")
  private[log] val logEntry = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
}

// The values NetLogo provides can be boxed, and we don't want to turn them into strings;
// we want `10` not `"10"` in the output.  Also, arrays are handled directly by the JSON
// library, so don't change them.  Everything else is stringified.  -Jeremy B June 2022
object AnyRefFormat {
  def forJson(value: AnyRef): AnyRef = {
    println(value)
    value match {
      case i: BoxedInt     => i
      case l: BoxedLong    => l
      case d: BoxedDouble  => d
      case b: BoxedBoolean => b
      case a: Array[_]     => a
      case s: String       => s
      case l: LogoList     => l.map(AnyRefFormat.forJson(_)).asJava
      case null            => null
      case v               => v.toString
    }
  }
}
