package io.exoquery.pprint

import io.exoquery.fansi.Str
import io.exoquery.pprint

data class Name(val first: String, val last: String)
data class Person(val name: Name, val age: Int)


fun main() {
  run {
    val p = Person(Name("Joe", "Bloggs"), 42)
    println(pprint(p))
  }

  //val seq = generateSequence { "foo" }
  ////val joined = Str.join(PPrinter.BlackWhite.tokenize(seq, height = 5).iterator())
  //val joined = pprint(seq)
//
  //`println(joined)
}
