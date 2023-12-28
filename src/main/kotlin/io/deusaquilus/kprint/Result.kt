package io.deusaquilus.kprint

import io.deusaquilus.fansi.Str

class Result(val iter: Sequence<Str>,
  val completedLineCount0: () -> Int,
  val lastLineLength0: () -> Int)
{
  val completedLineCount: Int by lazy {
    require(iter.none())
    completedLineCount0()
  }
  val lastLineLength: Int by lazy {
    require(iter.none())
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
        require(!iter.none())
        // In PPrint it's f(completedLineCount, lastLineLength0) but it's (lazy val lastLineLength0).
        // I think it would actually be executed at this point but I think thats fine because its supposed
        // to happen in the outer function call isMatch.
        val newResult = f(completedLineCount, lastLineLength0())
        newResult.iter.map { x ->
          if (!newResult.iter.any()) {
            newCompletedLineCount = newResult.completedLineCount
            newLastLineLength = newResult.lastLineLength
          }
          x
        }.iterator()
      }
    )
    return Result(
      mergedIterator.asSequence(),
      { newCompletedLineCount + completedLineCount }, // Kotlin has no direct by-name support so need to make it a lambda
      {
        if (newCompletedLineCount > 0) newLastLineLength
        else newLastLineLength + lastLineLength
      }
    )

  }

  companion object {
    fun fromString(s: () -> Str): Result {
      val lines by lazy { s().plainText.lineSequence().toList() }
      // Note that in PPrint this is new Result(Iterator(s), ...) which is not actually lazy!
      return Result(generateSequence(s), { lines.size - 1 }, { lines.last().length })
    }
  }
}



