package io.deusaquilus.pprint

import io.deusaquilus.fansi.Str

class Check(val width: Int = 100, val height: Int = 99999, val renderTwice: Boolean = false, val fields: Boolean = false){
  fun invoke(t: Any, vararg expected: String) = {

    val blackWhite = if (fields) Check.blackWhiteFields else Check.blackWhite
    val color = if (fields) Check.colorFields else Check.color
    val printers =
      if (!renderTwice) listOf(blackWhite)
      else listOf(blackWhite, color)
    // Make sure we
    for (pprinter in printers){
      val pprinted = Str.join(blackWhite.tokenize(t, width, height).asSequence().toList()).plainText

      assert(expected.map { it.trim() }.contains(pprinted))
    }
  }

  companion object {
    val blackWhite = PPrinter(defaultShowFieldNames = false)
    val color = PPrinter(defaultShowFieldNames = false)
    val blackWhiteFields = PPrinter()
    val colorFields = PPrinter()
  }
}
