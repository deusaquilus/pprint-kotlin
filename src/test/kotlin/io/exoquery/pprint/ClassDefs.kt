package io.exoquery.pprint

object Nested{
  object ODef { data class Foo(val i: Int, val s: String) }

  class CDef { data class Foo(val i: Int, val s: String)
    companion object { }
  }

}

object FooSingleton
data class Foo(val integer: Int, val sequence: List<String>)

data class FooG<T>(val t: T, val sequence: List<String>)
//data class FooNoArgs()


/*
 * A whole bunch of test data that can be used by client libraries to try out
 * their typeclass derivation to make sure it's doing the right thing. Contains
 * roughly the  whole range of interesting shapes of types supported by derive.
 */

object ADTs {
  class ADT0()
  data class ADTa(val i: Int)
  data class ADTb(val i: Int, val s: String)
  data class ADTc(val i: Int, val s: String, val t: Pair<Double, Double>)
  data class ADTd(val i: Int, val s: String, val t: Pair<Double, Double>, val a: ADTa)
  data class ADTe(val i: Int, val s: String, val t: Pair<Double, Double>, val a: ADTa, val q: List<Double>)
  data class ADTf(val i: Int, val s: String, val t: Pair<Double, Double>, val a: ADTa, val q: List<Double>, val o: Boolean?)
  data class ADTz(val t1: Int, val t2: String,
                  val t3: Int, val t4: String,
                  val t5: Int, val t6: String,
                  val t7: Int, val t8: String,
                  val t9: Int, val t10: String,
                  val t11: Int, val t12: String,
                  val t13: Int, val t14: String,
                  val t15: Int, val t16: String,
                  val t17: Int, val t18: String
                 )
}
object Hierarchy {
  sealed interface A
  data class B(val i: Int): A
  data class C(val s1: String, val s2: String): A

  sealed interface Z //new line
  object AnZ: Z //new line
}
object DeepHierarchy {
  sealed interface A
  data class B(val i: Int): A

  sealed interface C: A
  data class D(val s: String): C
  data class E(val b: Boolean): C

  sealed interface Q //new line
  data class AnQ(val i: Int): Q //new line

  data class F(val q: Q): C //new line


}