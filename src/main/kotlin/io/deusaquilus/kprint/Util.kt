package io.deusaquilus.kprint

object Util {
  fun escapeChar(c: Char, sb: StringBuilder, unicode: Boolean = true) = when (c) {

    // Have a look here: https://www.oreilly.com/library/view/android-development-with/9781787123687/edad3c06-152f-41a4-8080-0db1eb395680.xhtml
    // for a list of escape sequences in kotlin

    '\t' -> sb.append("\\t") // Tabulator
    '\b' -> sb.append("\\b") // Backspace
    '\n' -> sb.append("\\n") // Newline
    '\r' -> sb.append("\\r") // Carriage return
    '\'' -> sb.append("\\\'") // Quote
    '"' -> sb.append("\\\"")  // Double quote
    '\\' -> sb.append("\\\\") // Slash
    '\$' -> sb.append("\\$") // Dollar character

    // '\u' -> sb.append("\\$") // not legal and I don't think is needed
    //'\f' -> sb.append("\\f")  // no such escape sequence in kotlin?

    else ->
      if (c < ' ' || (c > '~' && unicode)) sb.append("\\u%04x".format(c.code))
      else sb.append(c)
  }

  fun literalize(s: CharArray, unicode: Boolean = true): String {
    val sb = StringBuilder()
    sb.append('"')
    var i = 0
    val len = s.size
    while (i < len) {
      Util.escapeChar(s[i], sb, unicode)
      i += 1
    }
    sb.append('"')

    return sb.toString()
  }
}