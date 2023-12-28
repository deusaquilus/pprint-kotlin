package io.deusaquilus.pprint

import io.deusaquilus.fansi.Attrs
import io.deusaquilus.fansi.Str
import io.deusaquilus.fansi.toStr

fun <T> Sequence<T>.hasNext(): Boolean = this.iterator().hasNext()
fun <T> Sequence<T>.next(): T = this.iterator().next()

/**
 * Basically like mkString, but for nested iterators. Used whenever
 * you want to put stuff "in between" the elements of the larger
 * iterator
 */
class Renderer(
  val maxWidth: Int,
  val colorApplyPrefix: Attrs,
  val colorLiteral: Attrs,
  val indentStep: Int
){

  fun rec(x: Tree, leftOffset: Int, indentCount: Int): Result = when(x) {
    is Tree.Apply -> {
      val prefix = x.prefix
      val body = x.body
      val nonEmpty = body.hasNext()

      // Render children and buffer them until you fill up a single line,
      // or you run out of children.
      //
      // Even before rendering any children, the indentation, prefix
      // and the two open/close parens already take up a few characters
      var totalHorizontalWidth = leftOffset + prefix.length + 2
      val buffer = mutableListOf<List<Str>>()
      var lastChildIter = sequenceOf<Str>()
      var childCompletedLineCount = 0
      while (body.hasNext() && totalHorizontalWidth <= maxWidth && childCompletedLineCount == 0) {

        val child = body.next()
        val childRes = rec(child, (indentCount + 1) * indentStep, indentCount + 1)

        val childBuffer = mutableListOf<Str>()
        while (childRes.iter.hasNext() && totalHorizontalWidth < maxWidth) {
          val next = childRes.iter.next()
          childBuffer += next
          totalHorizontalWidth += next.length
        }


        if (body.hasNext()) {
          totalHorizontalWidth += 2
        }

        if (!childRes.iter.hasNext()) {
          childCompletedLineCount = childCompletedLineCount + childRes.completedLineCount
        } else {
          lastChildIter = childRes.iter

        }

        buffer += childBuffer.toList()
      }

      fun applyHeader() = sequenceOf(colorApplyPrefix(prefix), Renderer.openParen)

      val indentPlusOne = Renderer.indent((indentCount + 1) * indentStep)

      fun separator() = sequenceOf(Renderer.commaNewLine, indentPlusOne)

      if (
        totalHorizontalWidth <= maxWidth &&
          childCompletedLineCount == 0 &&
          !lastChildIter.hasNext()
      ) {
        val iter = Util.concat(
          { applyHeader().iterator() },
          {
            Renderer.joinIter(
              buffer.asSequence().map { it.asSequence() },
              { sequenceOf(Renderer.commaSpace) }
            )
          },
          { sequenceOf(Renderer.closeParen).iterator() }
        )

        val length: Int = buffer.asSequence().map { it.asSequence().map { it.length }.sum() }.sum()
        Result(iter.asSequence(), {0}, {length})
      } else if (!nonEmpty && totalHorizontalWidth > maxWidth) {
        val iter = Util.concat(
          { applyHeader().iterator() },
          {sequenceOf(
            Renderer.newLine,
            Renderer.indent(indentCount * indentStep),
            Renderer.closeParen
          ).iterator()}
        )

        val length: Int = buffer.asSequence().map{it.asSequence().map {it.length}.sum()}.sum()
        Result(iter.asSequence(), {0}, {length})
      } else {
        fun bufferedFragments() = Renderer.joinIter(
          buffer.asSequence().withIndex().map { (i, v) ->
            if (i < buffer.size - 1) v.asSequence()
            else v.asSequence() + lastChildIter
          },
          { separator() }
        )

        fun nonBufferedFragments() = Renderer.joinIter(
          body.map { c -> rec(c, (indentCount + 1) * indentStep, indentCount + 1).iter },
          { separator() }
        )

        fun allFragments() =
          if (buffer.isEmpty()) nonBufferedFragments()
          else if (!body.hasNext()) bufferedFragments()
          else Renderer.joinIter(sequenceOf(bufferedFragments().asSequence(), nonBufferedFragments().asSequence()), { separator() })

        fun iter() = Util.concat(
          { applyHeader().iterator() },
          { sequenceOf(Renderer.newLine, indentPlusOne).iterator() },
          { allFragments() },
          { sequenceOf(
            Renderer.newLine,
            Renderer.indent(indentCount * indentStep),
            Renderer.closeParen
          ).iterator() }
        )


        Result(iter().asSequence(), {childCompletedLineCount + 2}, {indentCount * indentStep + 1})
      }
    }

    is Tree.Infix -> {
      val lhs = x.lhs
      val op = x.op
      val rhs = x.rhs
      rec(lhs, leftOffset, indentCount).flatMap { lhsNewline, lhsLastLineLength ->
        Result.fromString { " " + op + " " }.flatMap { _, _ ->
          rec(rhs, lhsLastLineLength, indentCount)
        }
      }
    }

    is Tree.Lazy -> {
      val t: Tree.Lazy = x
      val str by lazy {
        t.body0(Tree.Ctx(
          maxWidth, leftOffset, indentCount,
          indentStep, colorLiteral, colorApplyPrefix
        ))
      }
      Truncated(str.map { Str(it) }, maxWidth, height = 99999999).toResult()
    }

    is Tree.Literal -> Result.fromStr { colorLiteral(x.body) }

    is Tree.KeyValue -> {
      val k = x.key
      val v = x.value
      val prefix = "$k = "
      Result.fromString({ prefix })
        .flatMap { _, _ -> rec(v, leftOffset + prefix.length, indentCount) }
    }
  }

  companion object {
    fun <T> joinIter(it0: Sequence<Sequence<T>>, joiner: () -> Sequence<T>) =
      Util.ConcatIterator.fromSequences(it0, joiner)


    val openParen = Str("(")
    val closeParen = Str(")")
    val commaSpace = Str(", ")
    val newLine = Str("\n")
    val commaNewLine = Str(",\n")
    private val cachedIndents = (0 until 64).map { Str(" ".repeat(it)) }.toTypedArray()
    fun indent(n: Int) = if (n < 64) cachedIndents[n] else Str(" ".repeat(n))
  }
}
