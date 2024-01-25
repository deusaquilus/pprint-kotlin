package io.exoquery.pprint

import kotlinx.serialization.Serializable


object Nested{
  @Serializable
  object ODef { @Serializable data class Foo(val i: Int, val s: String) }

  @Serializable
  class CDef { @Serializable data class Foo(val i: Int, val s: String)
    companion object { }
  }

}

object FooSingleton

@Serializable
data class Foo(val integer: Int, val sequence: List<String>)

@Serializable
data class FooG<T>(val t: T, val sequence: List<String>)
//data class FooNoArgs()


/*
 * A whole bunch of test data that can be used by client libraries to try out
 * their typeclass derivation to make sure it's doing the right thing. Contains
 * roughly the  whole range of interesting shapes of types supported by derive.
 */

object ADTs {
  @Serializable
  data object ADT0
  @Serializable
  data class ADTa(val i: Int)
  @Serializable
  data class ADTb(val i: Int, val s: String)
  @Serializable
  data class ADTc(val i: Int, val s: String, val t: Pair<Double, Double>)
  @Serializable
  data class ADTd(val i: Int, val s: String, val t: Pair<Double, Double>, val a: ADTa)
  @Serializable
  data class ADTe(val i: Int, val s: String, val t: Pair<Double, Double>, val a: ADTa, val q: List<Double>)
  @Serializable
  data class ADTf(val i: Int, val s: String, val t: Pair<Double, Double>, val a: ADTa, val q: List<Double>, val o: Boolean?)
  @Serializable
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
  @Serializable
  sealed interface A
  data class B(val i: Int): A
  data class C(val s1: String, val s2: String): A

  sealed interface Z //new line
  object AnZ: Z //new line
}
object DeepHierarchy {
  @Serializable
  sealed interface A
  data class B(val i: Int): A

  @Serializable
  sealed interface C: A

  @Serializable
  data class D(val s: String): C

  @Serializable
  data class E(val b: Boolean): C

  @Serializable
  sealed interface Q //new line

  @Serializable
  data class AnQ(val i: Int): Q //new line

  @Serializable
  data class F(val q: Q): C //new line
}

object Singletons{
  @Serializable
  sealed interface AA
  @Serializable
  data object BB: AA
  @Serializable
  data object CC: AA

  @Serializable
  data object Standalone
}
object Generic{
  @Serializable
  data class A<T>(val t: T)
  @Serializable
  data class ADT<A, B, C, D, E, F>(val a: A, val b: B, val c: C, val d: D, val e: E, val f: F)
}
object Recursive{
  @Serializable
  sealed interface LL
  object End : LL
  data class Node(val c: Int, val next: LL): LL

  @Serializable
  data class IntTree(val value: Int, val children: List<IntTree>)

  @Serializable
  sealed interface SingleTree
  data class SingleNode(val value: Int, val children: List<SingleTree>): SingleTree
}

object Defaults {
  @Serializable
  data class ADTa(val i: Int = 0)
  @Serializable
  data class ADTb(val i: Int = 1, val s: String)
  @Serializable
  data class ADTc(val i: Int = 2, val s: String, val t: Pair<Double, Double> = Pair(1.toDouble(), 2.toDouble()))
}

interface MixedIn{
  interface Trt1{
    data class ClsA(val s: String)
  }
  interface Trt2: Trt1 {
    data class ClsB(val i: Int)
  }
  object Obj: Trt2

  // Equivalent of Scala's "object MixedIn: MixedIn"
  companion object {
  }
}


// Varargs not allowed in primary constructors of data classes
// object Varargs{
//   data class Sentence(val a: String, vararg val bs: String)
// }

object Covariant{
  data class Tree<out T>(val value: T)
}

object Exponential{
  @Serializable
  data class A1 (val x: A2, val y: A2)
  @Serializable
  data class A2 (val x: A3, val y: A3)
  @Serializable
  data class A3 (val x: A4, val y: A4)
  @Serializable
  data class A4 (val x: A5, val y: A5)
  @Serializable
  data class A5 (val x: A6, val y: A6)
  @Serializable
  data class A6 (val x: A7, val y: A7)
  @Serializable
  data class A7 (val x: A8, val y: A8)
  @Serializable
  data class A8 (val x: A9, val y: A9)
  @Serializable
  data class A9 (val x: A10, val y: A10)
  @Serializable
  data class A10(val x: A11, val y: A11)
  @Serializable
  data class A11(val x: A12, val y: A12)
  @Serializable
  data class A12(val x: A13, val y: A13)
  @Serializable
  data class A13(val x: A14, val y: A14)
  @Serializable
  data class A14(val x: A15, val y: A15)
  @Serializable
  data class A15(val x: A16, val y: A16)
  @Serializable
  data class A16(val x: A17, val y: A17)
  @Serializable
  data class A17(val x: A18, val y: A18)
  //data class A18() empty data-class not allowed in kotlin
  @Serializable
  object A18
}

object GenericADTs{
  @Serializable
  sealed interface Small<A>
  data class Small1<A>(val key: A): Small<A>

  @Serializable
  sealed interface Delta<out A, out B> {
    @Serializable
    data class Insert<A, B>(val key: A, val value: B): Delta<A, B>
    @Serializable
    data class Remove<A>(val key: A): Delta<A, Nothing>
    // Originally this was empty, can't have empty data-classes in Kotlin
    @Serializable
    data class Clear(val value: Int = 1): Delta<Nothing, Nothing>
  }

  @Serializable
  sealed interface DeltaInvariant<A, B> {
    @Serializable
    data class Insert<A, B>(val key: A, val value: B): DeltaInvariant<A, B>
    @Serializable
    data class Remove<A, B>(val key: A): DeltaInvariant<A, B>
    @Serializable
    data class Clear<A, B>(val value: Int = 1): DeltaInvariant<A, B>
  }

  @Serializable
  sealed interface DeltaHardcoded<A, B> {
    @Serializable
    data class Insert<A, B>(val key: A, val value: B): DeltaHardcoded<A, B>
    @Serializable
    data class Remove<A>(val key: A): DeltaHardcoded<A, String>
    @Serializable
    data class Clear(val value: Int = 1): DeltaHardcoded<List<Int>, String>
  }
}

object Amorphous{
  @Serializable
  class A()
  @Serializable
  data class B(val i: Int){
    val x = "lol"
  }
}

// ============ Issue originally from Scala, Not sure if this issue applies in Kotlin ============
// issue #95
// For some reason this stuff must live top-level; the test fails to
// go red when thedata classes are moved inside a wrapper object even
// when the fix is backed out
@Serializable
data class C1(val name : String, val types : List<String>)
@Serializable
data class C2(val results : List<C1>)

@Serializable
data class Result2(val name : String,
                   val whatever : String,
                   val types : List<String>
)

@Serializable
data class GeoCoding2(val results: List<Result2>, val status: String)

// ======== No such thing as ProductN in Kotlin so this issue cannot occur ========
//object Issue28{
//  class MyProduct2: Product2<String, Int> {
//    override fun _1: String = "asdf"
//    override fun _2: Int = 333
//    override fun canEqual(val that: Any): Boolean = false
//  }
//}

// ============ Issue originally from Scala, Not sure if this issue applies in Kotlin ============
object Issue94{
  @Serializable
  class Foo(val x: String){
    override fun toString() = x
    override fun hashCode() = x.hashCode()
    override fun equals(other: Any?) = when(other) {
      is Foo -> x == other.x
      else -> false
    }
  }

  @Serializable
  data class Example(val ids: List<Foo>)
  @Serializable
  data class Example2(val ids: List<List<Foo>>)

}

// ======== No such thing as Ordered in Kotlin so this issue cannot occur ========
//object Issue92{
//  abstract class Rational: Ordered<Rational>
//}

// ============ Issue originally from Scala, Not sure if this issue applies in Kotlin ============
object Issue96{
  @Serializable
  sealed interface Trait
  class BadApply(val i: Int): Trait {
    companion object {
      fun invoke(i: Int) = BadApply(i)
      fun invoke(i: Int, s: String) = BadApply(i + s.toInt())
    }
  }

  @Serializable
  sealed interface Field { }

  @Serializable
  data class ChoiceField(val choices: Array<String>): Field
}

// ============ Issue originally from Scala, Not relevant in Kotlin because Kotlin does not allow cyclic chain hiearchies ============
///**
// * Sample AST taken from the Scalatex project
// *
// * https://github.com/lihaoyi/Scalatex/
// *
// * It's a use case where eachdata class inherits from multiple distinct
// * sealed interfaces, which aren't a strict hierarchy
// */
//sealed interface Ast {
//  val offset: Int
//
//  /**
//   * @param parts The various bits of text and other things which make up this block
//   * @param offset
//   */
//  data class Block(val offset: Int, val parts: List<Block.Sub>): Chain.Sub, Block.Sub {
//    sealed interface Sub: Ast
//    data class Text(val offset: Int, val txt: String): Block.Sub
//    data class For(val offset: Int, val generators: String, val block: Block): Block.Sub
//    data class IfElse(val offset: Int, val condition: String, val block: Block, val elseBlock: Option<Block>): Block.Sub
//  }
//  data class Header(val offset: Int, val front: String, val block: Block): Block.Sub with Chain.Sub
//
//  /**
//   * @param lhs The first expression in this method-chain
//   * @param parts A list of follow-on items chained to the first
//   * @param offset
//   */
//  data class Chain(val offset: Int, val lhs: String, val parts: List<Chain.Sub>): Block.Sub {
//    sealed interface Sub: Ast
//    data class Prop(override val offset: Int, val str: String): Sub
//    data class TypeArgs(override val offset: Int, val str: String): Sub
//    data class Args(override val offset: Int, val str: String): Sub
//  }
//}


/**
 * Sample AST from the FastParse PythonParse project. Doesn't work yet
 *
 * A python abstract syntax tree
 *
 * Basically transcribed from https://docs.python.org/2/library/ast.html
 */
object PythonAst{

  @Serializable
  sealed interface expr {
    data class BoolOp(val op: Int, val values: expr): expr
    data class Yield(val value: expr?): expr
    data class Compare(val left: expr, val comparators: kotlin.collections.List<expr>): expr
    data class Call(val func: expr, val starargs: expr?): expr
    data class List(val elts: expr?, val ctx: expr_context): expr
  }

  @Serializable
  sealed interface expr_context {
    object Load: expr_context
  }

}
