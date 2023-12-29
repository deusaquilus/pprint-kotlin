package io.exoquery.pprint

import io.exoquery.fansi.Str
import io.exoquery.pprint

data class Person(val name: Name, val age: Int)

data class Name(val first: String, val last: String)

fun main() {
//  val p = Person(Name("Joe", "Bloggs"), 123)
//  val q = Person(Name("Quiggs", "Moggs"), 123)
//  println(PPrinter()(mapOf("key" to "value")))

  val seq = generateSequence { "foo" }
  //val joined = Str.join(PPrinter.BlackWhite.tokenize(seq, height = 5).iterator())
  val joined = pprint(seq)

  println(joined)
}
