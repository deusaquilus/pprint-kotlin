package io.exoquery.pprint

import io.exoquery.fansi.Color
import io.exoquery.fansi.Console.GREEN
import io.exoquery.fansi.Console.YELLOW
import io.exoquery.fansi.Str
import io.exoquery.kmp.pprint.PPrintSequenceSerializer
import io.exoquery.kmp.pprint.PPrinter
import kotlinx.serialization.serializer
import kotlin.test.Test

class VerticalTests {
  val Check = io.exoquery.pprint.Check(width = 25, height = 99999, renderTwice = true, fields = false)
  // =================== Single Nested Tests ===================

  // List streched to 3 lines
  @Test fun listStretchedTo3Lines() {
    io.exoquery.pprint.Check(width = 5, height = 99999, renderTwice = false, fields = false)(
      listOf(1, 2, 3),
      """List(
        |  1,
        |  2,
        |  3
        |)
      """.trimMargin()
    )
  }

  // List on one line
  @Test fun listOnOneLine() {
    Check(
      listOf("12", "12", "12"),
      """List("12", "12", "12")"""
    )
  }

  // Larger List on one line
  @Test fun largerListOnOneLine() {
    Check(
      listOf("123", "123", "123"),
      """List("123", "123", "123")"""
    )
  }

  // Uneven width List on multiple lines
  @Test fun unevenWidthListOnMultipleLines() {
    Check(
      listOf("1234", "123", "123"),
      """List(
        |  "1234",
        |  "123",
        |  "123"
        |)""".trimMargin()
    )
  }

  // Map on one line
  @Test fun mapOnOneLine() {
    Check(
      mapOf(1 to 2, 3 to 4),
      """Map(1 -> 2, 3 -> 4)"""
    )
  }

  // Maps to lists
  @Test fun mapsToLists() {
    Check(
      mapOf(listOf(1, 2) to listOf(3, 4), listOf(5, 6) to listOf(7, 8)),
      """Map(
        |  List(1, 2) -> List(3, 4),
        |  List(5, 6) -> List(7, 8)
        |)""".trimMargin()
    )

    // Multi line complex maps
    Check(
      mapOf(
        listOf(123, 456, 789, 123, 456) to listOf(3, 4, 3, 4),
        listOf(5, 6) to listOf(7, 8)
      ),
      """Map(
        |  List(
        |    123,
        |    456,
        |    789,
        |    123,
        |    456
        |  ) -> List(3, 4, 3, 4),
        |  List(5, 6) -> List(7, 8)
        |)""".trimMargin()
    )
  }

  // Maps to lists
  @Test fun mapsToLists2() {
    Check(
      mapOf(
        listOf(5, 6) to listOf(7, 8),
        listOf(123, 456, 789, 123, 456) to listOf(123, 456, 789, 123, 456)
      ),
      """Map(
        |  List(5, 6) -> List(7, 8),
        |  List(
        |    123,
        |    456,
        |    789,
        |    123,
        |    456
        |  ) -> List(
        |    123,
        |    456,
        |    789,
        |    123,
        |    456
        |  )
        |)""".trimMargin()
    )
  }

  // Multi line int list
  @Test fun multiLineIntList() {
    Check(
      listOf("12345", "12345", "12345"),
      """List(
        |  "12345",
        |  "12345",
        |  "12345"
        |)""".trimMargin()
    )
  }

  // Complex nested object
  @Test fun complexNestedObject() {
    Check(
      Foo(123, listOf("hello world", "moo")),
      """Foo(
        |  123,
        |  List(
        |    "hello world",
        |    "moo"
        |  )
        |)""".trimMargin()
    )
  }

  // Complex nested object one line
  @Test fun complexNestedObjectOneLine() {
    Check(
      Foo(123, listOf("moo")),
      """Foo(123, List("moo"))""".trimMargin()
    )
  }

  // =================== doubleNested Tests ===================
  @Test fun nestedSequence() {
    Check(
      listOf(listOf("omg", "omg"), listOf("mgg", "mgg"), listOf("ggx", "ggx")),
      """List(
        |  List("omg", "omg"),
        |  List("mgg", "mgg"),
        |  List("ggx", "ggx")
        |)""".trimMargin()
    )
  }

  @Test fun deeperNestedSequence() {
    Check(
      listOf(listOf("omg", "omg", "omg", "omg"), listOf("mgg", "mgg"), listOf("ggx", "ggx")),
      """List(
        |  List(
        |    "omg",
        |    "omg",
        |    "omg",
        |    "omg"
        |  ),
        |  List("mgg", "mgg"),
        |  List("ggx", "ggx")
        |)""".trimMargin()
    )
  }

  @Test fun evenDeeperNestedSequence() {
    Check(
      listOf(
        listOf(
          listOf("mgg", "mgg", "lols"),
          listOf("mgg", "mgg")
        ),
        listOf(
          listOf("ggx", "ggx"),
          listOf("ggx", "ggx", "wtfx")
        )
      ),
      """List(
        |  List(
        |    List(
        |      "mgg",
        |      "mgg",
        |      "lols"
        |    ),
        |    List("mgg", "mgg")
        |  ),
        |  List(
        |    List("ggx", "ggx"),
        |    List(
        |      "ggx",
        |      "ggx",
        |      "wtfx"
        |    )
        |  )
        |)""".trimMargin()
    )
  }

  // Nested sequence with collections
  @Test fun nestedSequenceWithCollections() {
    Check(
      FooG(
        listOf(FooG(arrayOf(Foo(123, listOf())), listOf())),
        listOf()
      ),
      """FooG(
        |  List(
        |    FooG(
        |      Array(
        |        Foo(123, List())
        |      ),
        |      List()
        |    )
        |  ),
        |  List()
        |)
      """.trimMargin()
    )
  }

  // More nested sequence with collections
  @Test fun moreNestedSequenceWithCollections() {
    Check(
      FooG(FooG(listOf(Foo(3, listOf())), listOf()), listOf()),
      """FooG(
        |  FooG(
        |    List(Foo(3, List())),
        |    List()
        |  ),
        |  List()
        |)""".trimMargin()
    )
  }

  // =================== traited Tests ===================
  @Test fun traited() {
    val Check = io.exoquery.pprint.Check()
    Check(Nested.ODef.Foo(2, "ba"), "Foo(2, \"ba\")")
    Check(Nested.CDef.Foo(2, "ba"), "Foo(2, \"ba\")")
  }

  // =================== Color Tests ===================
  @Test fun color() {
    fun countSubstring(str1: String, str2: String): Int {
      //@tailrec
      fun count(pos: Int, c: Int): Int {
        val idx = str1.indexOf(str2, pos)
        return if (idx == -1) c else count(idx + str2.length, c + 1)
      }
      return count(0, 0)
    }

    fun count(haystack: Iterator<Str>, vararg needles: Pair<String, Int>) {
      val str = haystack.asSequence().map { it.render() }.joinToString("")
      for ((needle, expected) in needles) {
        val count = countSubstring(str, needle)

        count shouldBe expected
      }
    }

    val cReset = Color.Reset.escape
    count(PPrinter.Color<Int>().tokenize(123), GREEN to 1, cReset to 1)
    count(PPrinter.Color<String>().tokenize(""), GREEN to 1, cReset to 1)
    count(PPrinter.Color<List<Int>>().tokenize(listOf(1, 2, 3)), GREEN to 3, YELLOW to 1, cReset to 4)
    count(
      PPrinter.Color<Map<Int, List<String>>>().tokenize(mapOf(1 to listOf(), 2 to listOf(" "), 3 to listOf("   "))),
      GREEN to 5, YELLOW to 4, cReset to 9
    )
  }

  // =================== Truncation Tests ===================


  @Test fun longNoTruncation_1000() {
    val CheckH15 = io.exoquery.pprint.Check(height = 15)
    CheckH15("a".repeat(1000), "\"" + "a".repeat(1000) + "\"")
  }
  @Test fun list100_30x100() {
    Check(
      Array(30, { 100 }).toList(),
        """List(
          |  100,
          |  100,
          |  100,
          |  100,
          |  100,
          |  100,
          |  100,
          |  100,
          |  100,
          |  100,
          |  100,
          |  100,
          |  100,
          |  100,
          |  100,
          |  100,
          |  100,
          |  100,
          |  100,
          |  100,
          |  100,
          |  100,
          |  100,
          |  100,
          |  100,
          |  100,
          |  100,
          |  100,
          |  100,
          |  100
          |)""".trimMargin()
    )
  }

  // ===================== ShortNonTruncated Tests =====================
  val CheckH15 = io.exoquery.pprint.Check(height = 15)
  @Test fun a1000() { CheckH15("a".repeat(1000), "\"" + "a".repeat(1000) + "\"") }
  @Test fun list1_2_3_4() { CheckH15(listOf(1,2,3,4), "List(1, 2, 3, 4)") }
  @Test fun listAsdfghjklqwertz13() {
    CheckH15(
      Array(13, {"asdfghjklqwertz"}).toList(),
        """List(
          |  "asdfghjklqwertz",
          |  "asdfghjklqwertz",
          |  "asdfghjklqwertz",
          |  "asdfghjklqwertz",
          |  "asdfghjklqwertz",
          |  "asdfghjklqwertz",
          |  "asdfghjklqwertz",
          |  "asdfghjklqwertz",
          |  "asdfghjklqwertz",
          |  "asdfghjklqwertz",
          |  "asdfghjklqwertz",
          |  "asdfghjklqwertz",
          |  "asdfghjklqwertz"
          |)
        """.trimMargin()
    )
  }

  // ===================== ShortLinesTruncated Tests =====================
  @Test fun listFoobarbaz13_truncated() {
    CheckH15(
      Array(15, { "foobarbaz" }).toList(),
      """List(
          |  "foobarbaz",
          |  "foobarbaz",
          |  "foobarbaz",
          |  "foobarbaz",
          |  "foobarbaz",
          |  "foobarbaz",
          |  "foobarbaz",
          |  "foobarbaz",
          |  "foobarbaz",
          |  "foobarbaz",
          |  "foobarbaz",
          |  "foobarbaz",
          |  "foobarbaz",
          |...""".trimMargin()
    )
  }

  @Test fun listFoobarbaz150_truncated() {
    CheckH15(
      Array(150, { "foobarbaz" }).toList(),
      """List(
          |  "foobarbaz",
          |  "foobarbaz",
          |  "foobarbaz",
          |  "foobarbaz",
          |  "foobarbaz",
          |  "foobarbaz",
          |  "foobarbaz",
          |  "foobarbaz",
          |  "foobarbaz",
          |  "foobarbaz",
          |  "foobarbaz",
          |  "foobarbaz",
          |  "foobarbaz",
          |...""".trimMargin()
      )
  }

  // ===================== LongLineTruncated Tests =====================
  @Test fun a_times_13_splitWidth() {
    // These print out one long line, but at the width that the
    // pretty-printer is configured to, it (including any trailing ...)
    // wraps to fit within the desired width and height
    val CheckW5H3 = io.exoquery.pprint.Check(width = 5, height = 3)
    CheckW5H3(
      "a".repeat(13),
      "\"aaaa" +
        "aaaaa" +
        "aaaa\""
    )
  }

  @Test fun a_times_1000_splitWidth_truncate() {
    val CheckW5H3 = io.exoquery.pprint.Check(width = 5, height = 3)
    CheckW5H3(
      "a".repeat(1000),
      "\"aaaa" +
        "aaaaa" +
        "..."
    )
  }

  @Test fun a_times_1000_splitWidth_truncate_largeWidth() {
    val CheckW60H5 = io.exoquery.pprint.Check(width = 60, height = 5)
    CheckW60H5(
      "a".repeat(1000),
      "\"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"+
        "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"+
        "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"+
        "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"+
        "..."
    )
  }

  @Test fun infiniteSequence() {
    val CheckH5 = io.exoquery.pprint.Check(height = 5)
    CheckH5.invoke(
      PPrintSequenceSerializer<String>(serializer<String>()),
      generateSequence { "foo" },
      """Sequence(
        |  "foo",
        |  "foo",
        |  "foo",
        |...
      """.trimMargin()
    )
  }

  @Test fun wrappedLines() {
    val Check = io.exoquery.pprint.Check(width = 5, height = 3)
    Check(
      "a".repeat(13),
      "\"aaaa" +
        "aaaaa" +
        "aaaa\""
    )
  }
}
