package io.exoquery.pprint

import io.exoquery.fansi.Attrs
//import java.util.*
//import kotlin.collections.LinkedHashMap
//import kotlin.reflect.full.allSuperclasses

/**
 * A lazy AST representing pretty-printable text. Models `foo(a, b)`
 * `foo op bar`, and terminals `foo` in both lazy and eager forms
 */
sealed interface Tree {
  val elementName: String?

  /**
   * Foo(aa, bbb, cccc)
   */
  data class Apply(val prefix: String, val body: Iterator<Tree>, override val elementName: String? = null): Tree

  /**
   * LHS op RHS
   */
  data class Infix(val lhs: Tree, val op: String, val rhs: Tree, override val elementName: String? = null): Tree

  /**
   * "xyz"
   */
  data class Literal(val body: String, override val elementName: String? = null): Tree{
    val hasNewLine = body.any { c -> c == '\n' || c == '\r' }
  }

  /**
   * x = y
   */
  data class KeyValue(val key: String, val value: Tree, override val elementName: String? = null): Tree

  /**
   * xyz
   */
  data class Lazy(val body0: (Ctx) -> Iterator<String>, override val elementName: String? = null): Tree

  data class Ctx(
    val width: Int,
    val leftOffset: Int,
    val indentCount: Int,
    val indentStep: Int,
    val literalColor: Attrs,
    val applyPrefixColor: Attrs
  )
}

abstract class EncodeHelper {
  abstract fun makeHexString(c: Char): String

  fun encodeChar(x: Char, escapeUnicode: Boolean, elementName: String?): Tree.Literal {
    val sb = StringBuilder()
    sb.append('\'')
    Util.escapeChar(x, sb, escapeUnicode, ::makeHexString)
    sb.append('\'')
    return Tree.Literal(sb.toString(), elementName)
  }

  fun encodeString(x: String, escapeUnicode: Boolean, elementName: String?): Tree.Literal {
    return if (x.any {c -> c == '\n' || c == '\r'}) Tree.Literal("\"\"\"" + x + "\"\"\"", elementName)
    else Tree.Literal(Util.literalize(x.toCharArray(), escapeUnicode, ::makeHexString), elementName)
  }
}



interface Walker<T> {
  val showGenericForCollections: Boolean

  // No additional handlers. To extend, override the treeify function
  // fun additionalHandlers: PartialFunction<Any, Tree>

  fun treeify(x: T, elementName: String?, escapeUnicode: Boolean, showFieldNames: Boolean): Tree
}


