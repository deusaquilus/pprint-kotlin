package io.exoquery.pprint

import io.exoquery.fansi.Str
import io.kotest.assertions.fail

class Check(val width: Int = 100, val height: Int = 99999, val renderTwice: Boolean = false, val fields: Boolean = false, val showGenericForCollections: Boolean = true){
  operator fun invoke(t: Any?, vararg expected: String?) {

    val blackWhite = if (fields) Check.blackWhiteFields else Check.blackWhite
    val color = if (fields) Check.colorFields else Check.color
    val printers =
      if (!renderTwice) listOf(blackWhite)
      else listOf(blackWhite, color)
    // Make sure we
    for (pprinter in printers){
      val pprinted = Str.join(blackWhite.tokenize(t, width, height).asSequence().toList()).plainText
      val expectedPrints = expected.map { it?.trim() }.toList()
      if (!expectedPrints.contains(pprinted)) {
        fail(
          """|========== The Expected List of Possibilities ==========
             |${expected.joinToString("------------------\n")}
             |========== Did not match the printed value ==========
             |${pprinted}
          """.trimMargin()
        )
      }
    }
  }

  companion object {
    val blackWhite = PPrinter(PPrinterConfig(defaultShowFieldNames = false))
    val color = PPrinter(PPrinterConfig(defaultShowFieldNames = false))
    val blackWhiteFields = PPrinter(PPrinterConfig())
    val colorFields = PPrinter(PPrinterConfig())
  }
}
