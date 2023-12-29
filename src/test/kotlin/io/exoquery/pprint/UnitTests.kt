package io.exoquery.pprint

import io.kotest.core.spec.style.FunSpec

class UnitTests : FunSpec({
  test("escapeChar"){
    fun check(c: Char, expected: String, unicode: Boolean = true) {
      val escaped = Util.escapeChar(c, StringBuilder(), unicode).toString()
      assert(escaped == expected)
    }
    check('a', "a")
    check('-', "-")
    check('\n', "\\n")
    check('\\', "\\\\")
    check('\t', "\\t")
    check('й', "\\u0439", true)
    check('й', "й", false)
  }
  test("literalize"){
    val simple = Util.literalize("hi i am a cow")
    val simpleExpected = """ "hi i am a cow" """.trim()
    assert(simple == simpleExpected)

    val escaped = Util.literalize("hi i am a \"cow\"")
    val escapedExpected = """ "hi i am a \"cow\"" """.trim()
    assert(escaped == escapedExpected)

    val withUnicodeStr = "with юникод"

    val withUnicodeEscaped = Util.literalize(withUnicodeStr, true)
    val withUnicodeEscapedExpected = "\"with \\u044e\\u043d\\u0438\\u043a\\u043e\\u0434\""
    assert(withUnicodeEscaped == withUnicodeEscapedExpected)

    val withUnicodeUnescaped = Util.literalize(withUnicodeStr, false)
    val withUnicodeUnescapedExpected = """ "with юникод" """.trim()
    assert(withUnicodeUnescaped == withUnicodeUnescapedExpected)
  }
  context("concatIter"){

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
      assert(output == expected)
    }
    test("multipleItems") {
      check(
        iteratorOf(
          iteratorOf(1, 2, 3),
          iteratorOf(4, 5, 6),
          iteratorOf(7, 8, 9)
        ),
        {
          count -= 1
          iteratorOf(count, count-1)
        },
        listOf(1, 2, 3, -1, -2, 4, 5, 6, -2, -3, 7, 8, 9)
      )
    }
    test("singleItem") {
      check(
        iteratorOf(
          iteratorOf(1, 2, 3)
        ),
        { iteratorOf(9) },
        listOf(1, 2, 3)
      )
    }

    test("empty") { check(
      iteratorOf(),
      { iteratorOf(9) },
      listOf()
    )}
    test("empty2") { check(
      iteratorOf(iteratorOf()),
      { iteratorOf(9) },
      listOf()
    )}
    test("joinedEmpties") { check(
      iteratorOf(iteratorOf(), iteratorOf()),
      { iteratorOf(9) },
      listOf(9)
    )}

    test("halfEmpty") { check(
      iteratorOf(iteratorOf(1), iteratorOf()),
      { iteratorOf(9) },
      listOf(1, 9)
    )}
    test("halfEmpty2") { check(
      iteratorOf(iteratorOf(), iteratorOf(1, 2, 3)),
      { iteratorOf(9) },
      listOf(9, 1, 2, 3)
    )}

  }
})