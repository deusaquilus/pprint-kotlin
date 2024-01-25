package io.exoquery.pprint

import io.exoquery.fansi.Str
import io.exoquery.fansi.toStr

/**
 * Wraps an input iterator of colored [<Str>]s, and produces the same
 * [<Str>]s but truncated once the wrapped-at-[[width]] text reaches
 * beyond a certain [[height]]
 */
class Truncated(
  val chunks0: Iterator<Str>,
  val width: Int,
  val height: Int,
  val truncationMarker: String = "..."): Iterator<Str> {
  // mutable.Buffer in Scala is a array-backed mutable list
  // MutableList is that in Kotlin. (in Scala MutableList is backed by a linked list so not the right case for it)
  val lineLengths = mutableListOf(0)

  private val Internal = object {

    val chunks = chunks0.asSequence().filter { it.length > 0 }.iterator()

    var previousSlashN = false
    var previousSlashR = false

    fun handleNormalChar(char: Char) {
      previousSlashN = false
      previousSlashR = false
      if (char == '\n' && previousSlashR || char == '\r' && previousSlashN) {
        // do nothing
      } else if (char == '\n') {
        previousSlashN = true
        lineLengths.add(0)
      } else if (char == '\r') {
        previousSlashR = true
        lineLengths.add(0)
      }
      else if (lineLengths.last() == width) lineLengths.add(1)
      else lineLengths[lineLengths.size - 1] += 1

    }

    val completedLines get() = lineLengths.size - 1

    var finishedChunk = false

    var lastLineFinished = false
    var lastChunkLeftover = Str("")

    fun consumeChunkUntilLine(chunk: Str, lineLimit: Int): Int? {
      var i = 0
      val chars = chunk.getChars()
      while (i < chars.size && completedLines < lineLimit) {
        val char = chars[i]
        handleNormalChar(char)
        i += 1
      }
      return if (i == chunk.length) null else i
    }

    var isTruncated0 = false
  }

  val completedLineCount get() = run {
    require(!hasNext())
    lineLengths.size - 1
  }

  val lastLineLength get() = run {
    require(!hasNext())
    lineLengths[lineLengths.size-1]
  }

  val isTruncated get() = run {
    require(!hasNext())
    Internal.isTruncated0
  }


  fun toResult() = Result(this, {completedLineCount}, {lastLineLength})

  override fun hasNext() = (Internal.chunks.hasNext() && Internal.completedLines < height - 1) || !Internal.lastLineFinished


  /**
   * [[Truncated]] streams the chunks one by one until it reaches the height
   * limit; then, it buffers up to one entire row worth of chunks to check
   * whether it overshoots. If it overshoots, it discards the chunks and prints
   * "..." instead. If not, the buffered chunks get printed all at once.
   */
  override fun next() = if (Internal.chunks.hasNext() && Internal.completedLines < height - 1) {
    val chunk = Internal.chunks.next()
    val i = Internal.consumeChunkUntilLine(chunk, height - 1)
    when {
      i == null -> {
        if (!Internal.chunks.hasNext()) {
          Internal.lastLineFinished = true
        }
        chunk
      }
      else -> {
        // chunk was partially consumed. This should only happen if the chunk
        // is overshooting the vertical limit

        // If the last line is not empty, it means there is a character
        // on that last line. In such a case
        val splitPoint = if (lineLengths.last() != 0) i - 1 else i
        Internal.lastChunkLeftover = chunk.substring(splitPoint, chunk.length)
        chunk.substring(0, splitPoint)
      }
    }

  } else if (!Internal.lastLineFinished) {
    val buffer = mutableListOf<Str>()
    var charsLeftOver = false
    val i = Internal.consumeChunkUntilLine(Internal.lastChunkLeftover, height)
    when {
      i == null -> buffer.add(Internal.lastChunkLeftover)
      else -> {
        charsLeftOver = true
        buffer.add(Internal.lastChunkLeftover.substring(0, i - 1))
      }
    }
    while(Internal.chunks.hasNext() && Internal.completedLines < height){
      val chunk = Internal.chunks.next()
      val i = Internal.consumeChunkUntilLine(chunk, height)
      when {
        i == null -> buffer.add(chunk)
        else -> {
          charsLeftOver = true
          buffer.add(chunk.substring(0, i))
        }
      }

    }

    Internal.lastLineFinished = true

    if (charsLeftOver || Internal.chunks.hasNext()) {
      Internal.isTruncated0 = true
      Str(truncationMarker)
    }
    else buffer.map { it.render() }.joinToString("").toStr()

  } else {
    throw NoSuchElementException("next on empty iterator")
  }
}

