package io.exoquery.pprint

import io.exoquery.fansi.Str
import io.exoquery.pprint
import java.time.format.DateTimeFormatter.ISO_LOCAL_DATE

data class Name(val first: String, val last: String)
data class Person(val name: Name, val age: Int)

data class Address(val street: String, val zip: Int)
data class Customer(val name: Name, val addresses: List<Address>)

data class Alias(val value: String)
data class ComplexCustomer(val name: Name, val addressAliases: Map<Alias, Address>)

data class VeryComplexCustomer(val name: Name, val addressAliases: Map<Alias, List<Address>>)

data class SequenceHolder(val seq: Sequence<String>)

fun ex1() = run {
  val p = Person(Name("Joe", "Bloggs"), 42)
  println(pprint(p))
}

fun ex2() = run {
  val p = Customer(Name("Joe", "Bloggs"), listOf(Address("foo", 123), Address("bar", 456), Address("baz", 789)))
  println(pprint(p))
}

fun ex3() = run {
  val p =
    ComplexCustomer(
      Name("Joe", "Bloggs"),
      mapOf(Alias("Primary") to Address("foo", 123), Alias("Secondary") to Address("bar", 456), Alias("Tertiary") to Address("baz", 789))
    )
  println(pprint(p))
}

fun ex4() = run {
  val p =
    VeryComplexCustomer(
      Name("Joe", "Bloggs"),
      mapOf(
        Alias("Primary") to
          listOf(Address("foo", 123), Address("foo1", 123), Address("foo2", 123)),
        Alias("Secondary") to
          listOf(Address("bar", 456), Address("bar1", 456), Address("bar2", 456)),
        Alias("Tertiary") to
          listOf(Address("baz", 789), Address("baz1", 789), Address("baz2", 789))
      )
    )
  println(pprint(p))
}

fun ex5() = run {
  var i = 0
  val p = SequenceHolder(generateSequence { "foo-${i++}" })
  println(pprint(p, defaultHeight = 10).plainText)
}

class CustomPPrinter1(config: PPrinterConfig) : PPrinter(config) {
  override fun treeify(x: Any?, escapeUnicode: Boolean, showFieldNames: Boolean): Tree =
    when (x) {
      is java.time.LocalDate -> Tree.Literal(x.format(ISO_LOCAL_DATE))
      else -> super.treeify(x, escapeUnicode, showFieldNames)
    }
}

fun ex7() = run {
  val pp = CustomPPrinter1(PPrinterConfig())
  println(pp.invoke(java.time.LocalDate.now()))
}

fun main() {
  ex7()


  //val seq = generateSequence { "foo" }
  ////val joined = Str.join(PPrinter.BlackWhite.tokenize(seq, height = 5).iterator())
  //val joined = pprint(seq)
//
  //`println(joined)
}
