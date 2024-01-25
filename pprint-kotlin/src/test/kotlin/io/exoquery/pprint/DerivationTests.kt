package io.exoquery.pprint

import io.kotest.core.spec.style.FunSpec

data class CustomToString(val value: Int = 1){
  override fun toString() = "LA LA LA"
}
sealed interface Customs {
  data class A(val i: Int): Customs
  data class B(val s: String): Customs{
    override fun toString() = "Beeee"
  }
}

object DerivationTests : FunSpec({

  val Check = Check(100, 99999, false, false)

  test("singletons"){
    Check(Singletons.Standalone, "Standalone")
    Check(Singletons.BB, "BB")
    Check(Singletons.CC, "CC")
    Check(Singletons.CC as Singletons.AA, "CC")
  }
  test("adts"){
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
  test("sealedHierarchies"){
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
  test("genericADTs"){
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

    Check(
      GenericADTs.DeltaHardcoded.Remove(listOf(1, 2, 3)) as GenericADTs.DeltaHardcoded<kotlin.collections.List<Int>, String>,
      """Remove(List(1, 2, 3))"""
    )
    Check(
      GenericADTs.Delta.Insert(listOf("omg", "wtf"), Pair(1, 0.2)) as GenericADTs.Delta<List<String>, Pair<Int, Double>>,
    """Insert(List("omg", "wtf"), Pair(1, 0.2))""",
    """Insert(List("omg", "wtf"), Pair(1, 0.2F))""",
    """Insert(List("omg", "wtf"), Pair(1, 0.200000))"""
    )
    Check(
      GenericADTs.DeltaInvariant.Clear<Int, String>() as GenericADTs.DeltaInvariant<Int, String>,
      """Clear(1)"""
    )

  }


  test("fallback"){
    // make sure we can pprint stuff that looks nothing like a data class
    // by falling back to good old toString
    val a =  Amorphous.A()
    Check(a, a.toString())
    Check(a as Any, a.toString())
    Check(listOf("lol", 1, 'c'), """List("lol", 1, 'c')""")
    Check(Triple("lol", 1, 'c') as Any, """Triple("lol", 1, 'c')""")

    // Even random non-Kotlin stuff should work
    val x = java.util.Random()
    Check(x, x.toString())
    val z = java.util.UUID(0, -1)
    Check(z, "00000000-0000-0000-ffff-ffffffffffff")
    // Make sure when dealing with composite data structures, we continue
    // to use the static versions as deep as we can go before falling back
    // to toString
    Check(
      Generic.ADT(x, x as java.io.Serializable, "lol", "lol" as Any, Pair(1.5, 2.5), Pair(1.5, 2.5) as Any),
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
  test("enums"){
    val days1 =
      PPrinter.BlackWhite.tokenize(
        java.util.concurrent.TimeUnit.DAYS
      ).asSequence().joinToString("")

    // No 2nd case in Kotlin
    //val days2 = pprint.PPrinter.BlackWhite.tokenize(
    //  scala.concurrent.duration.SECONDS as java.util.concurrent.TimeUnit
    //).joinToString("")

    assert(
      days1 == "DAYS"
      //days2 == "SECONDS"
    )
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

  test("test"){
    Check(
      C2(listOf(C1("hello", listOf("world")))),
      """C2(List(C1("hello", List("world"))))"""
    )
  }

})
