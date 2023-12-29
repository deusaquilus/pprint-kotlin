package io.deusaquilus.pprint

import io.deusaquilus.fansi.Attrs
import io.deusaquilus.fansi.Str
import io.deusaquilus.fansi.toStr

//fun <T> Sequence<T>.hasNext(): Boolean = this.iterator().hasNext()
//fun <T> Sequence<T>.next(): T = this.iterator().next()

/**
 * Basically like mkString, but for nested iterators. Used whenever
 * you want to put stuff "in between" the elements of the larger
 * iterator
 *
 * Kotlin Note: In general I have chosen to represent things with Iterators but I need to use
 * .asSequence() when I want to perform map, flatMap, filter etc... operators on the interator
 * because Kotlin iterators don't support these operators. The other option would be to make everything
 * a Sequence instead of an iterator but the problem with that is that Sequence does not support a
 * the .next() and .hasNext() operators. Now we could go down to the Sequence.iterator() level and
 * call these operators but the problem with that is if you go back up to an iterator by doing
 * Sequence.iterator().{stuff}.asSequence() and then do .iterator() on the result of that again,
 * you can only call the .iterator() on it once. This is because Iterator<T>.asSequence() calls a
 * `Sequence { this }.constrainOnce()` operation which creates a sequence whose .iterator() function
 * can only be call once (have a look at Sequences.kt for more info). This make sense because
 * if you mess around calling methods of an iterator of a once-only sequence unexpected things
 * will happen to that sequence. The only probelm is that the we are doing .asSequence() on that
 * ConcatIterator which means we can't do concatIterator.asSequence().{stuff}.hasNext() and then .next()
 * on it. The way to work around that would be to write a custom .asSequenceMulti operator on ConcatIterator
 * which returns a sequence whose iterator can be returned many times. This might be fine with how
 * ConcatIterator works. If there are problems with the current approach of converting to a sequence
 * whenver a .map/filter/flatMap call is needed we can take a look at that approach.
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
      var lastChildIter = sequenceOf<Str>().iterator()
      var childCompletedLineCount = 0
      while (body.hasNext() && totalHorizontalWidth <= maxWidth && childCompletedLineCount == 0) {

        val child = body.next()
        val childRes = rec(child, (indentCount + 1) * indentStep, indentCount + 1)

//        run {
//          val res = rec(child, (indentCount + 1) * indentStep, indentCount + 1)
//          println(res.iter.toList())
//          println("here")
//        }

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

      fun separator() = sequenceOf(Renderer.commaNewLine, indentPlusOne).iterator()

      if (
        totalHorizontalWidth <= maxWidth &&
          childCompletedLineCount == 0 &&
          !lastChildIter.hasNext()
      ) {
        val iter = Util.concat(
          { applyHeader().iterator() },
          {
            Renderer.joinIter(
              buffer.asSequence().map { it.iterator() }.iterator(),
              { sequenceOf(Renderer.commaSpace).iterator() }
            )
          },
          { sequenceOf(Renderer.closeParen).iterator() }
        )

        val length: Int = buffer.asSequence().map { it.asSequence().map { it.length }.sum() }.sum()
        Result(iter, {0}, {length})
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
        Result(iter, {0}, {length})
      } else {
        fun bufferedFragments() = Renderer.joinIter(
          buffer.asSequence().withIndex().map { (i, v) ->
            if (i < buffer.size - 1) v.iterator()
            else (v.asSequence() + lastChildIter.asSequence()).iterator()
          }.iterator(),
          { separator() }
        )

        fun nonBufferedFragments() = Renderer.joinIter(
          body.asSequence().map { c -> rec(c, (indentCount + 1) * indentStep, indentCount + 1).iter }.iterator(),
          { separator() }
        )

        fun allFragments() =
          if (buffer.isEmpty()) nonBufferedFragments()
          else if (!body.hasNext()) bufferedFragments()
          else Renderer.joinIter(sequenceOf(bufferedFragments(), nonBufferedFragments()).iterator(), { separator() })

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


        Result(iter(), {childCompletedLineCount + 2}, {indentCount * indentStep + 1})
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
      Truncated(str.asSequence().map { Str(it) }.iterator(), maxWidth, height = 99999999).toResult()
    }

    is Tree.Literal ->
      Result.fromStr { colorLiteral(x.body) }

    is Tree.KeyValue -> {
      val k = x.key
      val v = x.value
      val prefix = "$k = "
      Result.fromString({ prefix })
        .flatMap { _, _ -> rec(v, leftOffset + prefix.length, indentCount) }
    }
  }

  companion object {
    fun <T> joinIter(it0: Iterator<Iterator<T>>, joiner: () -> Iterator<T>) =
      Util.ConcatIterator(it0, joiner)


    val openParen = Str("(")
    val closeParen = Str(")")
    val commaSpace = Str(", ")
    val newLine = Str("\n")
    val commaNewLine = Str(",\n")
    private val cachedIndents = (0 until 64).map { Str(" ".repeat(it)) }.toTypedArray()
    fun indent(n: Int) = if (n < 64) cachedIndents[n] else Str(" ".repeat(n))
  }
}
