package io.deusaquilus.pprint

data class Person(val name: Name, val age: Int)

data class Name(val first: String, val last: String)

fun main() {
  val p = Person(Name("Joe", "Bloggs"), 123)
  val q = Person(Name("Quiggs", "Moggs"), 123)
  println(PPrinter()("foo" to q))
}
