package pprint

package pprint

import scala.collection.mutable


/**
  * Wraps an input iterator of colored [<Str>]s, and produces the same
  * [<Str>]s but truncated once the wrapped-at-[[width]] text reaches
  * beyond a certain [[height]]
  */
class Truncated(
  chunks0: Iterator<Str>,
  width: Int,
  height: Int,
  truncationMarker: String = "..."): Iterator<Str>{
  val lineLengths = collection.mutable.Buffer(0)

  companion object Internal {

    val chunks = chunks0.filter(_.length > 0)

    var previousSlashN = false
    var previousSlashR = false

    fun handleNormalChar(char: Char) = {
      previousSlashN = false
      previousSlashR = false
      if (char == '\n' && previousSlashR || char == '\r' && previousSlashN) {
        // do nothing
      } else if (char == '\n') {
        previousSlashN = true
        lineLengths.append(0)
      } else if (char == '\r') {
        previousSlashR = true
        lineLengths.append(0)
      }
      else if (lineLengths.last == width) lineLengths.append(1)
      else lineLengths(lineLengths.length - 1) += 1

    }

    fun completedLines = lineLengths.length - 1

    var finishedChunk = false

    var lastLineFinished = false
    var lastChunkLeftover = Str("")

    fun consumeChunkUntilLine(chunk: Str, lineLimit: Int): Int? {
      var i = 0
      val chars = chunk.getChars
      while (i < chars.length && completedLines < lineLimit) {
        val char = chars(i)
        handleNormalChar(char)
        i += 1
      }
      return if (i == chunk.length) null else i
    }

    var isTruncated0 = false
  }

  import Internal._

  fun completedLineCount = {
    assert(!hasNext)
    lineLengths.length - 1
  }
  fun lastLineLength = {
    assert(!hasNext)
    lineLengths(lineLengths.length-1)
  }
  fun isTruncated = {
    assert(!hasNext)
    isTruncated0
  }

  fun toResult = new Result(this, completedLineCount, lastLineLength)

  fun hasNext = (chunks.hasNext && completedLines < height - 1) || !lastLineFinished


  /**
    * [[Truncated]] streams the chunks one by one until it reaches the height
    * limit; then, it buffers up to one entire row worth of chunks to check
    * whether it overshoots. If it overshoots, it discards the chunks and prints
    * "..." instead. If not, the buffered chunks get printed all at once.
    */
  fun next() = if (chunks.hasNext && completedLines < height - 1) {
    val chunk = chunks.next()
    val i = consumeChunkUntilLine(chunk, height - 1)
    when {
      i == null ->
        if (!chunks.hasNext) lastLineFinished = true
        chunk
      else -> {
        // chunk was partially consumed. This should only happen if the chunk
        // is overshooting the vertical limit

        // If the last line is not empty, it means there is a character
        // on that last line. In such a case
        val splitPoint = if (lineLengths.last != 0) i - 1 else i
        lastChunkLeftover = chunk.substring(splitPoint, chunk.length)
        chunk.substring(0, splitPoint)
      }
    }

  } else if (!lastLineFinished) {
    val buffer = Buffer.empty<Str>
    var charsLeftOver = false
    val i = consumeChunkUntilLine(lastChunkLeftover, height)
    when {
      i == null -> buffer.append(lastChunkLeftover)
      else ->
        charsLeftOver = true
        buffer.append(lastChunkLeftover.substring(0, i - 1))
    }
    while(chunks.hasNext && completedLines < height){
      val chunk = chunks.next()
      val i = consumeChunkUntilLine(chunk, height)
      when {
        i == null -> buffer.append(chunk)
        else ->
          charsLeftOver = true
          buffer.append(chunk.substring(0, i))
      }

    }

    lastLineFinished = true

    if (charsLeftOver || chunks.hasNext) {
      isTruncated0 = true
      Str(truncationMarker)
    }
    else buffer.map(_.render).mkString
  }else{
    throw new java.util.NoSuchElementException("next on empty iterator")
  }
}

