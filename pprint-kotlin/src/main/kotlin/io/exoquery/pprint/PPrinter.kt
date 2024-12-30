package io.exoquery.pprint

import io.exoquery.fansi.Attrs
import java.util.SortedMap
import kotlin.reflect.full.*

object EncodeHelperImpl: EncodeHelper() {
  override fun makeHexString(c: Char): String = "\\u%04x".format(c.code)
}

open class PPrinter(override val config: PPrinterConfig = PPrinterConfig()): PPrinterBase<Any?>(config) {

  open override fun treeify(x: Any?, elementName: String?, escapeUnicode: Boolean, showFieldNames: Boolean): Tree {
    fun treeifySame(x: Any?) = treeify(x, elementName, escapeUnicode, showFieldNames)
    fun treeifyAtom(x: Any?) = treeify(x, null, escapeUnicode, showFieldNames)

    fun <T> applyArray(name: String, seq: Sequence<T>) =
      Tree.Apply(name, seq.map {x -> treeifySame(x)}.iterator(), elementName)

    return when {

      x == null -> Tree.Literal("null", elementName)
      x is Boolean -> Tree.Literal(x.toString(), elementName)
      x is Char -> EncodeHelperImpl.encodeChar(x, escapeUnicode, elementName)
      x is Byte -> Tree.Literal(x.toString(), elementName)
      x is Short -> Tree.Literal(x.toString(), elementName)
      x is Int -> Tree.Literal(x.toString(), elementName)
      x is Long -> Tree.Literal(x.toString() + "L", elementName)
      x is Float -> Tree.Literal(x.toString() + "F", elementName)
      x is Double -> Tree.Literal(x.toString(), elementName)
      x is String -> EncodeHelperImpl.encodeString(x, escapeUnicode, elementName)

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
            listOf(Tree.Infix(treeify(k, null, escapeUnicode, showFieldNames), "->", treeify(v, null, escapeUnicode, showFieldNames), null))
          }.iterator(),
          elementName
        )
      }

      // Note: Maybe want to have a configuration to always make it just "Sequence"
      x is Sequence<*> -> {
        val name = if (showGenericForCollections) "Sequence" else x::class.simpleName ?: "Map"
        Tree.Apply(name, x.asSequence().map { x -> treeify(x, null, escapeUnicode, showFieldNames) }.iterator(), elementName)
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
        Tree.Apply(name, x.asSequence().map { x -> treeify(x, null, escapeUnicode, showFieldNames) }.iterator(), elementName)
      }

      // No None in kotlin
      //case None -> Tree.Literal("None")

      x is Iterator<*> -> {
        if (!x.hasNext())
          Tree.Literal("empty iterator", null)
        else
          Tree.Literal("non-empty iterator", null)
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

      x is Sequence<*> -> Tree.Apply("Sequence", x.map {x -> treeifyAtom(x)}.iterator(), elementName)

      x is Pair<*, *> -> {
        // We could also do Tree.Infix(treeifySame(x.first), "to", treeifySame(x.second)), so it would be "a to b" not sure if that is better or worse
        Tree.Apply("Pair", sequenceOf(treeifyAtom(x.first), treeifyAtom(x.second)).iterator(), elementName)
      }

      x is Triple<*, *, *> ->
        Tree.Apply("Triple", sequenceOf(treeifySame(x.first), treeifyAtom(x.second), treeifyAtom(x.third)).iterator(), elementName)

      (x::class.isData) -> {
        val cls = x::class
        val className = cls.simpleName ?: cls.toString()
        val productArity = cls.constructors.firstOrNull()?.parameters?.size ?: 0

        // If there are no constructors it's usually a `data object`
        if (productArity == 0) Tree.Lazy({ ctx -> sequenceOf(x.toString()).iterator() }, elementName)
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
          Tree.Apply(className, ProductSupport.treeifyProductElements(x, cls, this, escapeUnicode, showFieldNames), elementName)
        }
      }

      else -> Tree.Lazy({ ctx ->
          val v = x.toString()
          //when {
          //  x.toString() == null -> "null" // It appears that in Kotlin x.toString() can never be null and we checked x for nullity at the top of this function
          //  else -> x.toString()
          //}
          sequenceOf(v).iterator()
        },
        elementName
      )
    }
  }

  companion object {
    val Color = PPrinter(PPrinterConfig())
    val BlackWhite = PPrinter(
      PPrinterConfig().copy(
        colorLiteral = Attrs.Empty,
        colorApplyPrefix = Attrs.Empty
      )
    )
  }
}
