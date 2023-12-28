package io.deusaquilus.pprint

object Util {

  fun <T> concat(vararg isMatch: (() -> Iterator<T>)) = ConcatIterator(isMatch.asSequence().map { it.invoke() }.iterator() /*(_())*/, { emptySequence<T>().iterator() })

  /**
   * Basically a fast, efficient `.flatten` or `mkString` for nested iterators
   *
   * For some reason, the funault way of concatenation e.g.
   *
   * val middle = first ++ lastChildIter ++ sep ++ remaining
   *
   * Was throwing weird NullPointerExceptions I couldn't figure out =(
   *
   * Also, ++ didn't seem to be sufficiently lazy, so it was forcing
   * things earlier than it really needed to. It isn't documented anywhere
   * how lazy it's meant to be, whereas `ConcatIterator` here is obviously
   * lazy and won't even evaluate each iterator until you ask it to
   */
  class ConcatIterator<T>(val it0: Iterator<Iterator<T>>, val joiner: () -> Iterator<T>): Iterator<T>{
    private var head: Iterator<T>? = null
    private var count = 0

    // tailrec
    private fun check(): Boolean =
      if (head != null && head?.hasNext() ?: false) true
      else if (!it0.hasNext()) false
      else {
        if (count % 2 == 0) head = it0.next()
        else head = joiner()
        count += 1

        check()
      }

    override fun hasNext() = check()

    /*
    There is an inherent assumption in this class that hasNext() will always be called
    and the it0.hasNext() will always return false when the `head` is null. If `next()`
    is called afterward a null-pointer exception can be thrown. The only way to solve
    this is to create a state such that:
    sealed trait State
    data class CheckTrue(it: Iterator<T>)
    object CheckFalse
    Then when check() would be called here and it does CheckFalse() then an exception
    is thrown. Doing all of these things however likely imposes some performance penanties
    unless transparent objects are used. Should look into this.
     */
    override fun next():T {
      check()
      return head?.next() ?: throw RuntimeException("Concat iterator error, head was null")
    }
  }

  fun isOperator(ident: String) = {
    (ident.length > 0) && (when (ident[0]) {
      '<' -> true
      '~' -> true
      '!' -> true
      '@' -> true
      '#' -> true
      '%' -> true
      '^' -> true
      '*' -> true
      '+' -> true
      '-' -> true
      /*'<' | */
      '>' -> true
      '?' -> true
      ':' -> true
      '=' -> true
      '&' -> true
      '|' -> true
      '/' -> true
      '\\' -> true
      else -> false
    })
  }

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