package io.exoquery.pprint

import io.exoquery.fansi.Attrs
import java.util.*
import kotlin.collections.LinkedHashMap
import kotlin.reflect.full.allSuperclasses

/**
 * A lazy AST representing pretty-printable text. Models `foo(a, b)`
 * `foo op bar`, and terminals `foo` in both lazy and eager forms
 */
sealed interface Tree {

  /**
   * Foo(aa, bbb, cccc)
   */
  data class Apply(val prefix: String, val body: Iterator<Tree>): Tree

  /**
   * LHS op RHS
   */
  data class Infix(val lhs: Tree, val op: String, val rhs: Tree): Tree

  /**
   * "xyz"
   */
  data class Literal(val body: String): Tree{
    val hasNewLine = body.any { c -> c == '\n' || c == '\r' }
  }

  /**
   * x = y
   */
  data class KeyValue(val key: String, val value: Tree): Tree

  /**
   * xyz
   */
  data class Lazy(val body0: (Ctx) -> Iterator<String>): Tree

  data class Ctx(
    val width: Int,
    val leftOffset: Int,
    val indentCount: Int,
    val indentStep: Int,
    val literalColor: Attrs,
    val applyPrefixColor: Attrs
  )
}

interface Walker {
  val showGenericForCollections: Boolean

  // No additional handlers. To extend, override the treeify function
  // fun additionalHandlers: PartialFunction<Any, Tree>

  fun treeify(x: Any?, escapeUnicode: Boolean, showFieldNames: Boolean): Tree {
    fun treeifySame(x: Any?) = treeify(x, escapeUnicode, showFieldNames)

    fun <T> applyArray(name: String, seq: Sequence<T>) =
      Tree.Apply(name, seq.map {x -> treeifySame(x)}.iterator())

    return when {

      x == null -> Tree.Literal("null")
      x is Boolean -> Tree.Literal(x.toString())
      x is Char -> {
        val sb = StringBuilder()
        sb.append('\'')
        Util.escapeChar(x, sb, escapeUnicode)
        sb.append('\'')
        Tree.Literal(sb.toString())
      }
      x is Byte -> Tree.Literal(x.toString())
      x is Short -> Tree.Literal(x.toString())
      x is Int -> Tree.Literal(x.toString())
      x is Long -> Tree.Literal(x.toString() + "L")
      x is Float -> Tree.Literal(x.toString() + "F")
      x is Double -> Tree.Literal(x.toString())
      x is String -> {
        if (x.any {c -> c == '\n' || c == '\r'}) Tree.Literal("\"\"\"" + x + "\"\"\"")
        else Tree.Literal(Util.literalize(x.toCharArray(), escapeUnicode))
      }

      // No Symbol in Kotlin
      //x is Symbol -> Tree.Literal("'" + x.name)

      x is Map<*, *> -> {
        val name =
          if (showGenericForCollections) {
            val superTypes = x::class.allSuperclasses.toSet()
            when {
              (SortedMap::class in superTypes) -> "SortedMap"
              (LinkedHashMap::class in superTypes) -> "LinkedHashMap"
              else -> "Map"
            }
          } else {
            x::class.simpleName ?: "Map"
          }
        Tree.Apply(
          name,
          x.asSequence().flatMap { (k, v) ->
            listOf(Tree.Infix(treeify(k, escapeUnicode, showFieldNames), "->", treeify(v, escapeUnicode, showFieldNames)))
          }.iterator()
        )
      }

      // Note: Maybe want to have a configuration to always make it just "Sequence"
      x is Sequence<*> -> {
        val name = if (showGenericForCollections) "Sequence" else x::class.simpleName ?: "Map"
        Tree.Apply(name, x.asSequence().map { x -> treeify(x, escapeUnicode, showFieldNames) }.iterator())
      }

      // Note: Maybe want to have a configuration to always make it just "Iterable"
      x is Iterable<*> -> {
        val name =
          if (showGenericForCollections) {
            val superTypes = x::class.allSuperclasses.toSet()
            when {
              (List::class in superTypes) -> "List"
              (Set::class in superTypes) -> "Set"
              else -> x::class.simpleName ?: "Iterable"
            }
          } else {
            x::class.simpleName ?: "Iterable"
          }
        Tree.Apply(name, x.asSequence().map { x -> treeify(x, escapeUnicode, showFieldNames) }.iterator())
      }

      // No None in kotlin
      //case None -> Tree.Literal("None")

      x is Iterator<*> -> {
        if (!x.hasNext())
          Tree.Literal("empty iterator")
        else
          Tree.Literal("non-empty iterator")
      }

      x is BooleanArray -> applyArray("BooleanArray", x.asSequence())
      x is ByteArray -> applyArray("ByteArray", x.asSequence())
      x is CharArray -> applyArray("CharArray", x.asSequence())
      x is DoubleArray -> applyArray("DoubleArray", x.asSequence())
      x is FloatArray -> applyArray("FloatArray", x.asSequence())
      x is IntArray -> applyArray("IntArray", x.asSequence())
      x is LongArray -> applyArray("LongArray", x.asSequence())
      x is ShortArray -> applyArray("ShortArray", x.asSequence())

      x is Array<*> -> applyArray("Array", x.asSequence())

      x is Sequence<*> -> Tree.Apply("Sequence", x.map {x -> treeifySame(x)}.iterator())

      x is Pair<*, *> -> {
        // We could also do Tree.Infix(treeifySame(x.first), "to", treeifySame(x.second)), so it would be "a to b" not sure if that is better or worse
        Tree.Apply("Pair", sequenceOf(treeifySame(x.first), treeifySame(x.second)).iterator())
      }

      x is Triple<*, *, *> ->
        Tree.Apply("Triple", sequenceOf(treeifySame(x.first), treeifySame(x.second), treeifySame(x.third)).iterator())

      (x::class.isData) -> {
        val cls = x::class
        val className = cls.simpleName ?: cls.toString()
        val productArity = cls.constructors.firstOrNull()?.parameters?.size ?: 0

        // If there are no constructors it's usually a `data object`
        if (productArity == 0) Tree.Lazy { ctx -> sequenceOf(x.toString()).iterator() }
        // Don't do "a to b" when product has 2 elements since there's a specific Pair/Triple in Kotlin
        //else if(productArity == 2 && Util.isOperator(x.productPrefix)){
        //  Tree.Infix(
        //    treeify(x.productElement(0), escapeUnicode, showFieldNames),
        //    x.productPrefix,
        //    treeify(x.productElement(1), escapeUnicode, showFieldNames)
        //  )
        // }
        //else (className.startsWith(tuplePrefix), className.lift(tuplePrefix.length)) match{
        //  // leave out tuple1, so it gets printed as Tuple1(foo) instead of (foo)
        //  // Don't check the whole suffix, because of specialization there may be
        //  // funny characters after the digit
        //  case (true, Some('2' | '3' | '4' | '5' | '6' | '7' | '8' | '9')) ->
        //    Tree.Apply("", x.productIterator.map(x -> treeify(x, escapeUnicode, showFieldNames)))
        //  case _ ->
        //    Tree.Apply(x.productPrefix, ProductSupport.treeifyProductElements(x, this, escapeUnicode, showFieldNames))
        //}
        else {
          Tree.Apply(className, ProductSupport.treeifyProductElements(x, cls, this, escapeUnicode, showFieldNames))
        }
      }

      else -> Tree.Lazy { ctx ->
        val v = x.toString()
          //when {
          //  x.toString() == null -> "null" // It appears that in Kotlin x.toString() can never be null and we checked x for nullity at the top of this function
          //  else -> x.toString()
          //}
        sequenceOf(v).iterator()
      }
    }
  }


}


