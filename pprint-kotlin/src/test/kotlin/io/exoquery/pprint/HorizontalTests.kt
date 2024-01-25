package io.exoquery.pprint

import io.kotest.core.spec.style.FunSpec

class HorizontalTests : FunSpec({
  val Check = io.exoquery.pprint.Check(9999)
  val CheckNonGeneric = io.exoquery.pprint.Check(9999, showGenericForCollections = false)

  context("Horizontal"){
    context("primitives"){
      // Can't instantiate this in kotlin
      //context("Unit"){
      //  test("") { Check((), "()", "undefined") }
      //}
      context("Char"){
        test("\\n") { Check('\n', "'\\n'") }
        test("'a'") { Check('a', "'a'") }
      }
      context("Byte"){
        test("123 Byte") { Check(123.toByte(), "123") }
        test("-123 Byte") { Check(-123.toByte(), "-123") }
      }
      context("Short"){
        test("123 Short") { Check(123.toShort(), "123") }
        test("-12345 Short") { Check(-12345.toShort(), "-12345") }
      }
      context("Int"){
        test("123 Int") { Check(123, "123") }
        test("-1234567 Int") { Check(-1234567, "-1234567") }
      }
      context("Long"){
        test("123456789012345L") { Check(123456789012345L, "123456789012345L") }
        test("-123456789012345L") { Check(-123456789012345L, "-123456789012345L") }
      }
      context("Float"){
        test("0.75F") { Check(0.75F, "0.75F", "0.750000F") }
        test("-13.5F") { Check(-13.5F, "-13.5F", "-13.500000F") }
      }
      context("Double"){
        test("0.125") { Check(0.125, "0.125", "0.125F", "0.125000") }
        test("-0.125") { Check(-0.125, "-0.125", "-0.125F", "-0.125000") }
      }
      context("String"){
        val tq = "\"\"\""
        test("i am a cow") { Check("i am a cow", """ "i am a cow" """) }
        test(""""hello"""") { Check( """ "hello" """.trim(), """ "\"hello\"" """.trim()) }

        test("Check \\n") { Check("\n", """
        |$tq
        |$tq
        """.trimMargin()) }
        test("Check \\n\\n\\n") { Check("\n\n\n", """
        |$tq
        |
        |
        |$tq
        """.trimMargin()) }
        val n = 1000
        test("Check newline with other text") { Check(
          "\n" + "ABCDEFG".repeat(n),
          "\"\"\"\n" + "ABCDEFG".repeat(n) + "\"\"\""
        ) }
      }
      // No symbols in kotlin
      // context("Symbols"){
      //   test - Check('hello, """'hello""")
      //   test - Check('I_AM_A_COW, """'I_AM_A_COW""")
      // }
    }

    context("misc"){
      //test("Nothing") - intercept[Exception](Check(throw new Exception(), ""))
      test("Null"){
        Check(null, "null")
        Check(null as String?, "null")
        Check(listOf("look!", null as String?, "hi"), """List("look!", null, "hi")""")
      }
      // No either in Kotlin
      // test("Either"){
      //   Check(Left(123): Either[Int, Int], "Left(123)")
      //   Check(Left(123): Left[Int, Int], "Left(123)")
      //   Check(Left(123), "Left(123)")
      //   Check(Right((1, "2", 3)), """Right((1, "2", 3))""")
      // }
      // No option in Kotlin
      // test("Options"){
      //   Check(Some(123), "Some(123)")
      //   Check(None: Option[Int], "None")
      //   Check(None: Option[Nothing], "None")
      //   Check(None, "None")
      //   Check(Some(None), "Some(None)")
      // }
      test("Default"){
        val baos = java.io.ByteArrayOutputStream()
        Check(baos, baos.toString())

      }
    }

    context("collections"){
      data class ExmapleValue(val iter: Iterator<Int>)
      // Fallback to toString
      test("Iterator"){
        Check(iteratorOf<String>(), "empty iterator", "<iterator>")
        Check(iteratorOf(1, 2, 3), "non-empty iterator", "<iterator>")
        Check(ExmapleValue(iteratorOf(1, 2, 3)), "ExmapleValue(non-empty iterator)", "ExmapleValue(<iterator>)")
      }

      test("Iterator") { Check(listOf('1', '2', '3'), "List('1', '2', '3')") }

      test("Array") { Check(arrayOf(1, 2, 3), "Array(1, 2, 3)") }

      test("BooleanArray") { Check(booleanArrayOf(true, false, true), "BooleanArray(true, false, true)") }
      test("ByteArray") { Check(byteArrayOf(1, 2, 3), "ByteArray(1, 2, 3)") }
      test("CharArray") { Check(charArrayOf('a', 'b', 'c'), "CharArray('a', 'b', 'c')") }
      test("DoubleArray") { Check(doubleArrayOf(1.0, 2.0, 3.0), "DoubleArray(1.0, 2.0, 3.0)") }
      test("FloatArray") { Check(floatArrayOf(1.0F, 2.0F, 3.0F), "FloatArray(1.0F, 2.0F, 3.0F)") }
      test("IntArray") { Check(intArrayOf(1, 2, 3), "IntArray(1, 2, 3)") }
      test("LongArray") { Check(longArrayOf(1L, 2L, 3L), "LongArray(1L, 2L, 3L)") }
      test("ShortArray") { Check(shortArrayOf(1, 2, 3), "ShortArray(1, 2, 3)") }

      test("Seq") { Check(listOf(1, 2, 3), "List(1, 2, 3)") }
      test("List") { Check(listOf("1", "2", "3"), """List("1", "2", "3")""") }
      //test("Vector") { Check(Vector('omg, 'wtf, 'bbq), """Vector('omg, 'wtf, 'bbq)""") }

      test("Buffer") { Check(
        mutableListOf("omg", "wtf", "bbq"),
        """List("omg", "wtf", "bbq")"""
      ) }


      // Streams are hard-coded to always display vertically, in order
      // to make streaming pretty-printing sane
      // test("Stream") { Check(
      //   Stream('omg, 'wtf, 'bbq),
      //   """Stream('omg, 'wtf, 'bbq)"""
      // ) }

      // No constructors for these in Kotlin directly
      //test("Iterable") { Check(Iterable('omg, 'wtf, 'bbq), """List('omg, 'wtf, 'bbq)""") }
      //test("Traversable") { Check(Traversable('omg, 'wtf, 'bbq), """List('omg, 'wtf, 'bbq)""") }
      //  test("Set") { Check(Set('omg), """Set('omg)""") }

      test("mutableSet") { Check(mutableSetOf("omg"), """Set("omg")""", """HashSet("omg")""") }
      test("collectionSet") { Check(setOf("omg"), """Set("omg")""") }
        test("SortedSet") { Check(
        sortedSetOf("1", "2", "3"),
        """TreeSet("1", "2", "3")""",
        """Set("1", "2", "3")"""
      ) }
      test("Map"){
        Check(mapOf("key" to "value"), """Map("key" -> "value")""")
      }
      test("collectionMap"){
        Check(mapOf("key" to "value"), """Map("key" -> "value")""")
      }

        test("mutableMap"){
        Check(
          mutableMapOf("key" to "value"),
        """Map("key" -> "value")""",
        """HashMap("key" -> "value")"""
        )
      }

      test("SortedMap") { Check(
        sortedMapOf("key" to "v", "key2" to "v2"),
        """Map("key" -> "v", "key2" -> "v2")""",
        """SortedMap("key" -> "v", "key2" -> "v2")"""
        ) }
      }

        // TODO Pair and Triple

        //cateogry("tuples"){
        //  test("normal"){
        //    Check(Tuple1("123"), """Tuple1("123")""")
        //    Check((1, 2, "123"), """(1, 2, "123")""")
        //    Check(
        //
        //      (1, 2, "123", (100L, 200L), 1.5F, 0.1),
        //      """(1, 2, "123", (100L, 200L), 1.5F, 0.1)""",
        //      """(1, 2, "123", (100L, 200L), 1.5F, 0.1F)""",
        //      """(1, 2, "123", (100L, 200L), 1.500000F, 0.100000)"""
        //    )
        //  }
        //  test("infix"){
        //    case class ::(x: Any, y: Any)
        //    Check(::(1, 2), "1 :: 2")
        //    Check(::(0, ::(1, 2)), "0 :: 1 :: 2")
        //  }
        //}
  }
})