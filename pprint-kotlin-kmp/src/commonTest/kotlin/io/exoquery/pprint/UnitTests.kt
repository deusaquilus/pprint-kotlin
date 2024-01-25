package io.exoquery.pprint

import io.exoquery.kmp.pprint.EncodeHelperImpl
import kotlin.test.Test

class UnitTests {
  @Test fun escapeChar() {
    fun check(c: Char, expected: String, unicode: Boolean = true) {
      val escaped = Util.escapeChar(c, StringBuilder(), unicode, EncodeHelperImpl::makeHexString).toString()
      escaped shouldBe expected
    }
    check('a', "a")
    check('-', "-")
    check('\n', "\\n")
    check('\\', "\\\\")
    check('\t', "\\t")
    check('й', "\\u0439", true)
    check('й', "й", false)
  }

  @Test fun literalize() {
   val simple = Util.literalize("hi i am a cow", true, EncodeHelperImpl::makeHexString)
    val simpleExpected = """ "hi i am a cow" """.trim()
    simple shouldBe simpleExpected

    val escaped = Util.literalize("hi i am a \"cow\"", true, EncodeHelperImpl::makeHexString)
    val escapedExpected = """ "hi i am a \"cow\"" """.trim()
    escaped shouldBe escapedExpected

    val withUnicodeStr = "with юникод"

    val withUnicodeEscaped = Util.literalize(withUnicodeStr, true, EncodeHelperImpl::makeHexString)
    val withUnicodeEscapedExpected = "\"with \\u044e\\u043d\\u0438\\u043a\\u043e\\u0434\""
    withUnicodeEscaped shouldBe withUnicodeEscapedExpected

    val withUnicodeUnescaped = Util.literalize(withUnicodeStr, false, EncodeHelperImpl::makeHexString)
    val withUnicodeUnescapedExpected = """ "with юникод" """.trim()
    withUnicodeUnescaped shouldBe withUnicodeUnescapedExpected
  }
}

class ConcatIterUnitTests {
    var count = 0
    fun check(
      iter: Iterator<Iterator<Int>>,
      joiner: () -> Iterator<Int>,
      expected: List<Int>
    ) {
      val joined = Util.ConcatIterator(
        iter,
        { joiner() }
      )
      val output = joined.asSequence().toList()
      output shouldBe expected
    }


  @Test fun multipleItems() {
    check(
      iteratorOf(
        iteratorOf(1, 2, 3),
        iteratorOf(4, 5, 6),
        iteratorOf(7, 8, 9)
      ),
      {
        count -= 1
        iteratorOf(count, count - 1)
      },
      listOf(1, 2, 3, -1, -2, 4, 5, 6, -2, -3, 7, 8, 9)
    )
  }

  @Test fun singleItem() {
    check(
      iteratorOf(
        iteratorOf(1, 2, 3)
      ),
      { iteratorOf(9) },
      listOf(1, 2, 3)
    )
  }

  @Test fun empty() {
    check(
      iteratorOf(),
      { iteratorOf(9) },
      listOf()
    )
  }

  @Test fun empty2() {
    check(
      iteratorOf(iteratorOf(), iteratorOf()),
      { iteratorOf(9) },
      listOf(9)
    )
  }

  @Test fun joinedEmpties() {
    check(
      iteratorOf(iteratorOf(), iteratorOf()),
      { iteratorOf(9) },
      listOf(9)
    )
  }

  @Test fun halfEmpty() {
    check(
      iteratorOf(iteratorOf(1), iteratorOf()),
      { iteratorOf(9) },
      listOf(1, 9)
    )
  }

  @Test fun halfEmpty2() {
    check(
      iteratorOf(iteratorOf(), iteratorOf(1, 2, 3)),
      { iteratorOf(9) },
      listOf(9, 1, 2, 3)
    )
  }
}
