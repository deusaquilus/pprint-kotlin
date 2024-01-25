@file:UseSerializers(PPrintIteratorSerializer::class)

package io.exoquery.pprint

import io.exoquery.fansi.Str
import io.exoquery.kmp.pprint.PPrintIteratorSerializer
import io.exoquery.kmp.pprint.PPrinter
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import kotlinx.serialization.serializer

data class CheckFailedException(val msg: String): Exception(msg)

class Check(val width: Int = 100, val height: Int = 99999, val renderTwice: Boolean = false, val fields: Boolean = false, val showGenericForCollections: Boolean = true){
  class Serializers<T>(ser: SerializationStrategy<T>) {
    val blackWhite = PPrinter(ser, PPrinterConfig(defaultShowFieldNames = false))
    val color = PPrinter(ser, PPrinterConfig(defaultShowFieldNames = false))
    val blackWhiteFields = PPrinter(ser, PPrinterConfig())
    val colorFields = PPrinter(ser, PPrinterConfig())
  }

  inline operator fun <reified T> invoke(t: T, vararg expected: String?) =
    invoke(PPrinter.defaultModule.serializer<T>(), t, *expected)

  inline operator fun <reified T> invoke(serializer: SerializationStrategy<T>, t: T, vararg expected: String?) {
    val sers = Serializers<T>(serializer)

    val blackWhite = if (fields) sers.blackWhiteFields else sers.blackWhite
    val color = if (fields) sers.colorFields else sers.color
    val printers =
      if (!renderTwice) listOf(blackWhite)
      else listOf(blackWhite, color)
    // Make sure we
    for (pprinter in printers){
      val pprinted = Str.join(blackWhite.tokenize(t, width, height).asSequence().toList()).plainText
      val expectedPrints = expected.map { it?.trim() }.toList()
      if (!expectedPrints.contains(pprinted)) {
        throw CheckFailedException(
          """|========== The Expected List of Possibilities ==========
             |${expected.joinToString("------------------\n")}
             |========== Did not match the printed value ==========
             |${pprinted}
          """.trimMargin()
        )
      }
    }
  }

}
