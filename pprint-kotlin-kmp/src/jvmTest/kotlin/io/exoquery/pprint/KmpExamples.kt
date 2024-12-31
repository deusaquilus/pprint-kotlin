package io.exoquery.pprint

import io.exoquery.kmp.pprint.PPrintSequenceSerializer
import io.exoquery.kmp.pprint.PPrinter
import io.exoquery.kmp.pprint
import io.exoquery.kmp.pprint.PPrinterManual
import kotlinx.serialization.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Serializable
data class Name(val first: String, val last: String)

@Serializable
data class Person(val name: Name, val age: Int)

@Serializable
data class Stuff(val value: Map<String, String>)

@Serializable
data class IterStuff(val value: Iterator<Int>)

// Problematic Cases with GADTs 1
object GADT1 {
  @Serializable
  sealed interface Root<A, B>
  @Serializable
  data class PartiallyTyped<A>(val value: A): Root<A, String>

  fun <A> passValue(root: Root<A, String>): Root<A, String> = root

  fun gadt() {
    val value = passValue(PartiallyTyped(listOf(1,2,3)))
    println(pprint(value))
    //println(Json.encodeToString(value))
  }
}

// Problematic Cases with GADTs 2
object GADT2 {
  @Serializable
  sealed interface Root<A, B>
  @Serializable
  data class Parent<A, B>(val child: Root<A, B>): Root<A, B>
  @Serializable
  data class PartiallyTyped<A>(val value: A): Root<A, String>

  fun gadt() {
    val value = Parent(PartiallyTyped(listOf(1,2,3)))
    println(pprint(value))
    //println(Json.encodeToString(value))
  }
}


object UsingSequence1 {
  @Serializable
  data class Using(@Contextual val seq: Sequence<String>)
}

fun usingSequence0() {
  val using = UsingSequence1.Using(generateSequence { "foo" })
  println(pprint(using))
}

fun usingSequence1() {
  val using = UsingSequence1.Using(generateSequence { "foo" })
  val printer = PPrinter(UsingSequence1.Using.serializer())
  val p = printer(using)
  println(p)
}

object UsingSequence2 {
  @Serializable
  data class Using(@Serializable(with = PPrintSequenceSerializer::class) val seq: Sequence<Inside>)
  @Serializable
  data class Inside(val value: String)
}

fun usingSequence2() {
  val using = UsingSequence2.Using(generateSequence { UsingSequence2.Inside("foo") })
  val printer = PPrinter(UsingSequence2.Using.serializer())
  val p = printer(using)
  println(p)
}

object CustomPrinter {
  class CustomPPrinter1<T>(override val serializer: SerializationStrategy<T>, override val config: PPrinterConfig) : PPrinter<T>(serializer, config) {
    override fun <R> treeifyValueOrNull(value: R, elementName: String?, escapeUnicode: Boolean, showFieldNames: Boolean): Tree? =
      when (value) {
        is LocalDate -> Tree.Literal(value.format(DateTimeFormatter.ofPattern("MM/dd/YYYY")), elementName)
        else -> super.treeifyValueOrNull(value, elementName, escapeUnicode, showFieldNames)
      }
  }

  inline fun <reified T> myPPrint(value: T) = CustomPPrinter1(serializer<T>(), PPrinterConfig()).invoke(value)
}

fun customPrinter() {
  @Serializable
  data class Person(val name: String,  @Contextual val born: LocalDate)
  val pp = CustomPrinter.CustomPPrinter1(Person.serializer(), PPrinterConfig())
  val joe = Person("Joe", LocalDate.of(1981, 1, 1))

  println(pp.invoke(joe))

  println(CustomPrinter.myPPrint(joe))
}


// TODO SHOULD MAKE THIS INTO A UNITE TEST (can use small width like 10)
object MutualRecurse {
  @Serializable
  data class Foo(var bar: Bar?)
  @Serializable
  data class Bar(var foo: Foo?)
}

fun mutualRecurse() {
  val foo = MutualRecurse.Foo(null)
  val bar = MutualRecurse.Bar(null)
  foo.bar = bar
  bar.foo = foo
  val printer = PPrinter(MutualRecurse.Foo.serializer())
  val p = printer(foo)
  println(p)
}

fun showMap() {
  val p = Stuff(mapOf("foo" to "bar", "faa" to "baa"))
  val printer = PPrinter(Stuff.serializer())
  val str = printer(p)
  println(str)
}

fun showIteratorInObject() {
  val p = IterStuff(iteratorOf(1,2,3))
  val printer = PPrinter(IterStuff.serializer())
  val str = printer(p)
  println(str)
}

fun showPerson() {
  val per = Person(Name("Joe", "Bloggs"), 123)
  val printer = PPrinter(Person.serializer())
  val str = printer(per)
  println(str)
}

@Serializable
data class PersonBorn(val name: String, val born: Long)

class CustomPPrinter6<T>(override val serializer: SerializationStrategy<T>, override val config: PPrinterConfig) : PPrinter<T>(serializer, config) {
  override fun <E> treeifyComposite(elem: Treeifyable.Elem<E>, elementName: String?, showFieldNames: Boolean): Tree =
    when(elem.value) {
      is PersonBorn ->
        when (val p = super.treeifyComposite(elem, elementName, showFieldNames)) {
          is Tree.Apply -> p.copy(body = p.body.asSequence().toList().filter { it.elementName != "born" }.iterator())
          else -> error("Expected Tree.Apply")
        }
      else -> super.treeifyComposite(elem, elementName, showFieldNames)
    }
}

fun customPrinter6() {
  val joe = PersonBorn("Joe", 123)
  val printer = CustomPPrinter6<PersonBorn>(PersonBorn.serializer(), PPrinterConfig())
  val p = printer(joe)
  println(p)
}

@Serializable
sealed interface Colors {
  @Serializable object Red : Colors
  @Serializable object Green : Colors
  @Serializable object Blue : Colors
  @Serializable data class Custom(val value: String) : Colors
}

data class PersonFavorite(val name: String, val age: Int, val favoriteColor: Colors) /// See the Sealed Hierarchies section above

class ColorsPrinter(config: PPrinterConfig): PPrinter<Colors>(Colors.serializer(), config)

class CustomPPrinter7(config: PPrinterConfig): PPrinterManual<Any?>(config) {
  fun treeifyThis(x: Any?, elementName: String?) =
    treeify(x, elementName, config.defaultEscapeUnicode, config.defaultShowFieldNames)

  override fun treeify(x: Any?, elementName: String?, escapeUnicode: Boolean, showFieldNames: Boolean): Tree =
    when (x) {
      is PersonFavorite ->
        Tree.Apply(
          "PersonFavorite",
          iteratorOf(treeifyThis(x.name, "name"), treeifyThis(x.age, "age"), treeifyThis(x.favoriteColor, "favoriteColor")),
          elementName
        )
      is Colors -> ColorsPrinter(config).treeify(x, elementName, escapeUnicode, showFieldNames)
      else -> super.treeify(x, elementName, escapeUnicode, showFieldNames)
    }
}

fun customPrinter7() {
  val joe = PersonFavorite("Joe", 123, Colors.Custom("FF0000"))
  val printer = CustomPPrinter7(PPrinterConfig(showGenericForCollections = true))
  val p = printer(joe)
  println(p)
}

@OptIn(ExperimentalSerializationApi::class)
fun main() {
//  showMap()
//  showPerson()
//  showIteratorInObject()
//  mutualRecurse()
//  usingSequence0()
//    usingSequence1()
//  customPrinter()

  // GADT1.gadt()
  //GADT2.gadt()
  //customPrinter6()
  customPrinter7()
}