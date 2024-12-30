package io.exoquery.pprint

import io.exoquery.kmp.pprint
import io.exoquery.kmp.pprint.PPrinter
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.test.Test
import kotlin.time.DurationUnit

data class CustomToString(val value: Int = 1){
  override fun toString() = "LA LA LA"
}
sealed interface Customs {
  data class A(val i: Int): Customs
  data class B(val s: String): Customs {
    override fun toString() = "Beeee"
  }
}



class DerivationTests {

  val Check = Check(100, 99999, false, false)

  @Test
  fun singletons() {
    Check(Singletons.Standalone, "Standalone")
    Check(Singletons.BB, "BB")
    Check(Singletons.CC, "CC")
    // Can't play games with types in kotlinx-serialization since it is generated at compile-time.
    // A type is what the compiler says it is.
    Check(Singletons.CC as Singletons.AA, "CC")
  }

  @Test
  fun adts() {
    Check(
      ADTs.ADTb(123, "hello world"),
      """ADTb(123, "hello world")"""
    )

    Check(
      listOf(ADTs.ADTb(123, "hello world"), ADTs.ADTb(-999, "i am cow")),
      """List(ADTb(123, "hello world"), ADTb(-999, "i am cow"))"""
    )

    Check(ADTs.ADT0, "ADT0")
  }

  @Test
  fun sealedHierarchies() {
    Check(
      DeepHierarchy.AnQ(1),
      "AnQ(1)"
    )
    Check(
      DeepHierarchy.AnQ(1) as DeepHierarchy.Q,
      "AnQ(1)"
    )
    Check(
      DeepHierarchy.E(false),
      "E(false)"
    )
    Check(
      DeepHierarchy.F(DeepHierarchy.AnQ(1)) as DeepHierarchy.A,
      "F(AnQ(1))"
    )
  }

  // Scenario doesn't exist in Kotlin
  // test("varargs"){
  //   import Varargs._
  //     Check(
  //       Sentence("omg", "2", "3"),
  //       """Sentence("omg", WrappedArray("2", "3"))""",
  //       """Sentence("omg", ArrayBuffer("2", "3"))""", // 2.10
  //       """Sentence("omg", ArraySeq("2", "3"))""", // 2.13
  //       """Sentence("omg", WrappedVarArgs("2", "3"))""" // Scala.JS 2.13
  //     )
  // }

  @Test
  fun genericADTs() {
    Check(GenericADTs.DeltaHardcoded.Remove("omg"), """Remove("omg")""")
    Check(
      GenericADTs.Delta.Insert(listOf("omg", "wtf"), Pair(1, 0.2)),
      """Insert(List("omg", "wtf"), Pair(1, 0.2))""",
      """Insert(List("omg", "wtf"), Pair(1, 0.2F))""",
      """Insert(List("omg", "wtf"), Pair(1, 0.200000))"""
    )
    Check(
      GenericADTs.DeltaInvariant.Clear<Int, String>(),
      """Clear(1)"""
    )
    Check(
      GenericADTs.DeltaInvariant.Clear<Int, String>(),
      """Clear(1)"""
    )

    // These tests below are not working due to the issue: https://github.com/Kotlin/kotlinx.serialization/issues/1341
    //
    //    Check(
    //      GenericADTs.DeltaHardcoded.Remove(listOf(1, 2, 3)) as GenericADTs.DeltaHardcoded<kotlin.collections.List<Int>, String>,
    //      """Remove(List(1, 2, 3))"""
    //    )
    //    Check(
    //      GenericADTs.Delta.Insert(listOf("omg", "wtf"), Pair(1, 0.2)) as GenericADTs.Delta<List<String>, Pair<Int, Double>>,
    //    """Insert(List("omg", "wtf"), Pair(1, 0.2))""",
    //    """Insert(List("omg", "wtf"), Pair(1, 0.2F))""",
    //    """Insert(List("omg", "wtf"), Pair(1, 0.200000))"""
    //    )
    //    Check(
    //      GenericADTs.DeltaInvariant.Clear<Int, String>() as GenericADTs.DeltaInvariant<Int, String>,
    //      """Clear(1)"""
    //    )

  }

  @Test
  fun fallback() {
    // make sure we can pprint stuff that looks nothing like a data class
    // by falling back to good old toString
    val a = Amorphous.A()
    Check(a, "A()")
    pprint(Triple("lol", 1, 'c'), defaultShowFieldNames = false).plainText shouldBe """Triple("lol", 1, 'c')"""

    val x = 123
    // Make sure when dealing with composite data structures, we continue
    // to use the static versions as deep as we can go before falling back
    // to toString
    Check(
      Generic.ADT(x, x, "lol", "lol", Pair(1.5, 2.5), Pair(1.5, 2.5)),
      """ADT($x, $x, "lol", "lol", Pair(1.5, 2.5), Pair(1.5, 2.5))""",
      """ADT($x, $x, "lol", "lol", Pair(1.5F, 2.5F), Pair(1.5F, 2.5F))""",
      """ADT(
         |  $x,
         |  $x,
         |  "lol",
         |  "lol",
         |  Pair(1.500000, 2.500000),
         |  Pair(1.500000, 2.500000)
         |)""".trimMargin(),
      """ADT(
         |  $x,
         |  $x,
         |  "lol",
         |  "lol",
         |  Pair(1.5, 2.5),
         |  Pair(1.5, 2.5)
         |)""".trimMargin()
    )
  }

  @Test
  fun enums() {
    val duratinUnitSeralizer =
      object : KSerializer<DurationUnit> {
        override val descriptor: SerialDescriptor = buildClassSerialDescriptor("DurationUnit") { }
        override fun serialize(encoder: Encoder, value: DurationUnit) = encoder.encodeString(value.toString())
        override fun deserialize(decoder: Decoder) =
          throw IllegalStateException("PPrint sequence serializer does not support deserialization.")
      }

    val days1 =
      PPrinter.BlackWhite<DurationUnit>(duratinUnitSeralizer).tokenize(DurationUnit.DAYS).asSequence().joinToString("")

    // No 2nd case in Kotlin
    //val days2 = pprint.PPrinter.BlackWhite.tokenize(
    //  scala.concurrent.duration.SECONDS as java.util.concurrent.TimeUnit
    //).joinToString("")

    days1 shouldBe "DurationUnit(\"DAYS\")"
    //days2 == "SECONDS"
  }

  // No such thing as ProductN in Kotlin
  // test("issue28"){
  //   val r = Issue28.MyProduct2
  //     Check(r : Issue28.MyProduct2, """("asdf", 333)""")
  // }

  // No such thing as Ordered in Kotlin
  // test("issue92"){
  //   val r = Issue92.Rational {
  //     override fun compare(that: Issue92.Rational): Int = TODO()
  //   }
  //   Check(r as Issue92.Rational, r.toString())
  // }

  @Test
  fun test() {
    Check(
      C2(listOf(C1("hello", listOf("world")))),
      """C2(List(C1("hello", List("world"))))"""
    )
  }
}
