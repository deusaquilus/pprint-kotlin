package io.exoquery.pprint

import io.exoquery.fansi.Color
import io.exoquery.fansi.Console.GREEN
import io.exoquery.fansi.Console.YELLOW
import io.exoquery.fansi.Str
import io.kotest.core.spec.style.FunSpec

class VerticalTests : FunSpec({

  context("Vertical"){
    val Check = io.exoquery.pprint.Check(width = 25, height = 99999, renderTwice = true, fields = false)
    context("singleNested"){
      test("List streched to 3 lines") { io.exoquery.pprint.Check(width = 5, height = 99999, renderTwice = false, fields = false)(
        listOf(1, 2, 3),
        """List(
          |  1,
          |  2,
          |  3
          |)
        """.trimMargin()
      ) }
      test("List on one line") {Check(
        listOf("12", "12", "12"),
        """List("12", "12", "12")"""
      ) }
      test("Larger List on one line") { Check(
        listOf("123", "123", "123"),
        """List("123", "123", "123")"""
      ) }
      test("Uneven width List on multiple lines") { Check(
        listOf("1234", "123", "123"),
        """List(
          |  "1234",
          |  "123",
          |  "123"
          |)""".trimMargin()
      ) }
      test("Map on one line") { Check(
        mapOf(1 to 2, 3 to 4),
        """Map(1 -> 2, 3 -> 4)"""
      ) }
      test("Maps to lists") { Check(
        mapOf(listOf(1, 2) to listOf(3, 4), listOf(5, 6) to listOf(7, 8)),
        """Map(
          |  List(1, 2) -> List(3, 4),
          |  List(5, 6) -> List(7, 8)
          |)""".trimMargin()
      ) }

      test("Multi line complex maps") { Check(
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
      ) }

      test("Maps to lists") { Check(
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
      ) }

      test("Muti line int list") { Check(
        listOf("12345", "12345", "12345"),
        """List(
          |  "12345",
          |  "12345",
          |  "12345"
          |)""".trimMargin()
      ) }
      test("Complex nested object") { Check(
        Foo(123, listOf("hello world", "moo")),
        """Foo(
          |  123,
          |  List(
          |    "hello world",
          |    "moo"
          |  )
          |)""".trimMargin()
      ) }
      test("Complex nested object one line") { Check(
        Foo(123, listOf("moo")),
        """Foo(123, List("moo"))""".trimMargin()
      ) }

    }
    context("doubleNested"){

      test("Nested sequence") { Check(
        listOf(listOf("omg", "omg"), listOf("mgg", "mgg"), listOf("ggx", "ggx")),
        """List(
          |  List("omg", "omg"),
          |  List("mgg", "mgg"),
          |  List("ggx", "ggx")
          |)""".trimMargin()
      ) }
      test("Deeper Nested sequence") { Check(
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
      ) }
      test("Even Deeper Nested sequence") { Check(
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
      ) }
      test("Nested sequence with collections") { Check(
        FooG(listOf(FooG(arrayOf(Foo(123, listOf())), listOf())),
          listOf()
        ),
        """FooG(
          |  Vector(
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
      ) }
      test("More nested sequence with collections") { Check(
        FooG(FooG(listOf(Foo(3, listOf())), listOf()), listOf()),
        """FooG(
          |  FooG(
          |    List(Foo(3, List())),
          |    List()
          |  ),
          |  List()
          |)""".trimMargin()
      ) }
    }
  }

  test("traited"){
    val Check = io.exoquery.pprint.Check()
    Check(Nested.ODef.Foo(2, "ba"), "Foo(2, \"ba\")")
    Check(Nested.CDef.Foo(2, "ba"), "Foo(2, \"ba\")")
  }

  context("Color"){
    fun countSubstring(str1:String, str2:String):Int {
      //@tailrec
      fun count(pos:Int, c:Int):Int {
        val idx=str1.indexOf(str2, pos)
        return if(idx == -1) c else count(idx+str2.length, c+1)
      }
      return count(0,0)
    }

    fun count(haystack: Iterator<Str>, vararg needles: Pair<String, Int>) {
      val str = haystack.asSequence().map { it.render() }.joinToString("")
      for ((needle, expected) in needles){
        val count = countSubstring(str, needle)

        assert(count == expected)
      }
    }

    // Need to find some way to make this import work
    val cReset = Color.Reset.escape
    test("Tokenize 123") { count(PPrinter.Color.tokenize(123), GREEN to 1, cReset to 1) }
    test("Tokenize Quotes") { count(PPrinter.Color.tokenize(""), GREEN to 1, cReset to 1) }
    test("Tokenize listOf(1, 2, 3)") { count(PPrinter.Color.tokenize(listOf(1, 2, 3)), GREEN to 3, YELLOW to 1, cReset to 4) }
    test("Tokenize complex map") { count(
      PPrinter.Color.tokenize(mapOf(1 to listOf(), 2 to listOf(" "), 3 to listOf("   "))),
      GREEN to 5, YELLOW to 4, cReset to 9
    ) }
  }



//  context("Truncation"){
//    context("longNoTruncation"){
//      val Check = io.exoquery.pprint.Check()
//      test { Check("a" * 10000,"\""+"a" * 10000+"\"") }
//      test { Check(
//        List.fill(30)(100),
//        """List(
//          |  100,
//          |  100,
//          |  100,
//          |  100,
//          |  100,
//          |  100,
//          |  100,
//          |  100,
//          |  100,
//          |  100,
//          |  100,
//          |  100,
//          |  100,
//          |  100,
//          |  100,
//          |  100,
//          |  100,
//          |  100,
//          |  100,
//          |  100,
//          |  100,
//          |  100,
//          |  100,
//          |  100,
//          |  100,
//          |  100,
//          |  100,
//          |  100,
//          |  100,
//          |  100
//          |)""".trimMargin()
//      ) }
//    }
//
//    test("shortNonTruncated"){
//      val Check = io.exoquery.pprint.Check(height = 15)
//      test { Check("a"*1000, "\"" + "a"*1000 + "\"") }
//      test { Check(List(1,2,3,4), "List(1, 2, 3, 4)") }
//      test { Check(
//        List.fill(13)("asdfghjklqwertz"),
//        """List(
//          |  "asdfghjklqwertz",
//          |  "asdfghjklqwertz",
//          |  "asdfghjklqwertz",
//          |  "asdfghjklqwertz",
//          |  "asdfghjklqwertz",
//          |  "asdfghjklqwertz",
//          |  "asdfghjklqwertz",
//          |  "asdfghjklqwertz",
//          |  "asdfghjklqwertz",
//          |  "asdfghjklqwertz",
//          |  "asdfghjklqwertz",
//          |  "asdfghjklqwertz",
//          |  "asdfghjklqwertz"
//          |)
//        """.trimMargin()
//      ) }
//    }
//
//    test("shortLinesTruncated"){
//      val Check = io.exoquery.pprint.Check(height = 15)
//      test { Check(
//        List.fill(15)("foobarbaz"),
//        """List(
//          |  "foobarbaz",
//          |  "foobarbaz",
//          |  "foobarbaz",
//          |  "foobarbaz",
//          |  "foobarbaz",
//          |  "foobarbaz",
//          |  "foobarbaz",
//          |  "foobarbaz",
//          |  "foobarbaz",
//          |  "foobarbaz",
//          |  "foobarbaz",
//          |  "foobarbaz",
//          |  "foobarbaz",
//          |...""".trimMargin()
//      ) }
//      test { Check(
//        List.fill(150)("foobarbaz"),
//        """List(
//          |  "foobarbaz",
//          |  "foobarbaz",
//          |  "foobarbaz",
//          |  "foobarbaz",
//          |  "foobarbaz",
//          |  "foobarbaz",
//          |  "foobarbaz",
//          |  "foobarbaz",
//          |  "foobarbaz",
//          |  "foobarbaz",
//          |  "foobarbaz",
//          |  "foobarbaz",
//          |  "foobarbaz",
//          |...""".trimMargin()
//      ) }
//    }
//
//    test("longLineTruncated"){
//      // These print out one long line, but at the width that the
//      // pretty-printer is configured to, it (including any trailing ...)
//      // wraps to fit within the desired width and height
//      test{
//        val Check = io.exoquery.pprint.Check(width = 5, height = 3)
//        Check(
//          "a" * 13,
//          "\"aaaa" +
//            "aaaaa" +
//            "aaaa\""
//        )
//      }
//      test{
//        val Check = io.exoquery.pprint.Check(width = 5, height = 3)
//        Check(
//          "a" * 1000,
//          "\"aaaa" +
//            "aaaaa" +
//            "..."
//        )
//      }
//      test{
//        val Check = io.exoquery.pprint.Check(width = 60, height = 5)
//        Check(
//          "a" * 1000,
//          "\"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"+
//            "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"+
//            "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"+
//            "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"+
//            "..."
//        )
//      }
//    }
//
//    test("stream"){
//      val Check = io.exoquery.pprint.Check(height = 5)
//      Check(
//        Stream.continually("foo"),
//        """Stream(
//          |  "foo",
//          |  "foo",
//          |  "foo",
//          |...
//        """.trimMargin()
//      )
//    }
//  }
//
//  test("wrappedLines"){
//    val Check = io.exoquery.pprint.Check(width = 8, height = 5)
//
//    Check(
//      "1234567890\n"*10,
//      "\"\"\"1234567890\n1234567890\n..."
//    )
//    // The result looks like 10 wide 3 deep, but because of the wrapping
//    // (maxWidth = 8) it is actually 8 wide and 5 deep.
//  }

})