package io.exoquery.pprint

import io.exoquery.fansi.Str
import io.exoquery.fansi.toStr

/**
 * The intermediate return type of the pretty-print system: provides an
 * iterator which produces the actual string output, as well as metadata
 * around that output that is only available after the iterator is exhausted
 */
class Result(
  val iter: Iterator<Str>,
  val completedLineCount0: () -> Int,
  val lastLineLength0: () -> Int)
{
  val completedLineCount: Int by lazy {
    require(!iter.hasNext())
    completedLineCount0()
  }
  val lastLineLength: Int by lazy {
    require(!iter.hasNext())
    lastLineLength0()
  }
  fun flatMap(f: (Int, Int) -> Result): Result {
    var newCompletedLineCount = 0
    var newLastLineLength = 0

    val mergedIterator = Util.concat(
      // () -> iter
      { iter.iterator() },
      // () -> { require... }
      {
        require(!iter.hasNext())
        // In PPrint it's f(completedLineCount, lastLineLength0) but it's (lazy val lastLineLength0).
        // I think it would actually be executed at this point but I think thats fine because its supposed
        // to happen in the outer function call isMatch.
        val newResult = f(completedLineCount, lastLineLength0())
        newResult.iter.asSequence().map { x ->
          if (!newResult.iter.hasNext()) {
            newCompletedLineCount = newResult.completedLineCount
            newLastLineLength = newResult.lastLineLength
          }
          x
        }.iterator()
      }
    )
    return Result(
      mergedIterator,
      { newCompletedLineCount + completedLineCount }, // Kotlin has no direct by-name support so need to make it a lambda
      {
        if (newCompletedLineCount > 0) newLastLineLength
        else newLastLineLength + lastLineLength
      }
    )

  }

  companion object {
    fun fromString(s: () -> String): Result = fromStr { s().toStr() }
    fun fromStr(s: () -> Str): Result {
      val lines by lazy { s().plainText.lineSequence().toList() }
      // Note that in PPrint this is new Result(Iterator(s), ...) which is not actually lazy!
      return Result(sequenceOf(s()).iterator(), { lines.size - 1 }, { lines.last().length })
    }
  }
}



