package io.exoquery.pprint

import io.exoquery.fansi.Str
import io.exoquery.pprint
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatter.*

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
      is java.time.LocalDate -> Tree.Literal(x.format(DateTimeFormatter.ofPattern("MM/dd/YYYY")))
      else -> super.treeify(x, escapeUnicode, showFieldNames)
    }
}

fun ex7() = run {
  data class Person(val name: String, val born: LocalDate)
  val pp = CustomPPrinter1(PPrinterConfig())
  println(pp.invoke(Person("Joe", LocalDate.of(1981, 1, 1))))
}

class MyJavaBean(val a: String, val b: Int) {
  fun getValueA() = a
  fun getValueB() = b
}

class CustomPPrinter2(config: PPrinterConfig) : PPrinter(config) {
  override fun treeify(x: Any?, esc: Boolean, names: Boolean): Tree =
    when (x) {
      is MyJavaBean -> Tree.Apply("MyJavaBean", listOf(x.getValueA(), x.getValueB()).map { treeify(it, esc, names) }.iterator())
      else -> super.treeify(x, esc, names)
    }
}

fun ex8() = run {
  val bean = MyJavaBean("abc", 123)
  val pp = CustomPPrinter2(PPrinterConfig())
  println(pp.invoke(bean))
}

class CustomPPrinter3(config: PPrinterConfig) : PPrinter(config) {
  override fun treeify(x: Any?, esc: Boolean, names: Boolean): Tree {
    // function to make recursive calls shorter
    fun rec(x: Any?) = treeify(x, esc, names)
    return when (x) {
      // Recurse on the values, pass result into Tree.KeyValue.
      is MyJavaBean ->
        Tree.Apply(
          "MyJavaBean",
          listOf(Tree.KeyValue("a", rec(x.getValueA())), Tree.KeyValue("b", rec(x.getValueB()))).iterator()
        )
      else ->
        super.treeify(x, esc, names)
    }
  }
}

fun ex9() = run {
  val bean = MyJavaBean("abc", 123)
  val pp = CustomPPrinter3(PPrinterConfig())
  println(pp.invoke(bean))
}


class CustomPPrinter4(config: PPrinterConfig) : PPrinter(config) {
  override fun treeify(x: Any?, escapeUnicode: Boolean, showFieldNames: Boolean): Tree {
    // function to make recursive calls shorter
    fun rec(x: Any?) = treeify(x, escapeUnicode, showFieldNames)
    fun field(fieldName: String, value: Any?) =
      if (showFieldNames) Tree.KeyValue(fieldName, rec(value)) else rec(value)
    return when (x) {
      // Recurse on the values, pass result into Tree.KeyValue.
      is MyJavaBean ->
        Tree.Apply("MyJavaBean", listOf(field("a", x.getValueA()), field("b", x.getValueB())).iterator())
      else ->
        super.treeify(x, escapeUnicode, showFieldNames)
    }
  }
}

fun ex10() = run {
  val bean = MyJavaBean("abc", 123)
  println(CustomPPrinter4(PPrinterConfig()).invoke(bean))
  println(CustomPPrinter4(PPrinterConfig(defaultShowFieldNames = false)).invoke(bean))
}

fun main() {
  ex10()


  //val seq = generateSequence { "foo" }
  ////val joined = Str.join(PPrinter.BlackWhite.tokenize(seq, height = 5).iterator())
  //val joined = pprint(seq)
//
  //`println(joined)
}
