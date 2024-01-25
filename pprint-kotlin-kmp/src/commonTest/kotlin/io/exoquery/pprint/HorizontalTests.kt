package io.exoquery.pprint//package io.exoquery.pprint

import io.exoquery.kmp.pprint.PPrintIteratorSerializer
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import kotlin.test.Test

class Primitives {
  val Check = io.exoquery.pprint.Check(9999)
  val CheckNonGeneric = io.exoquery.pprint.Check(9999, showGenericForCollections = false)

  @Test fun char() {
    Check('\n', "'\\n'")
    Check('a', "'a'")
  }
  @Test fun byte() {
    Check(123.toByte(), "123")
    Check(-123.toByte(), "-123")
  }
  @Test fun short() {
    Check(123.toShort(), "123")
    Check(-12345.toShort(), "-12345")
  }
  @Test fun int() {
    Check(123, "123")
    Check(-1234567, "-1234567")
  }
  @Test fun long() {
    Check(123456789012345L, "123456789012345L")
    Check(-123456789012345L, "-123456789012345L")
  }
  @Test fun float() {
    Check(0.75F, "0.75F", "0.750000F", "0.75")
    Check(-13.5F, "-13.5F", "-13.500000F", "-13.5")
  }
  @Test fun double() {
    Check(0.125, "0.125", "0.125F", "0.125000")
    Check(-0.125, "-0.125", "-0.125F", "-0.125000")
  }
  @Test fun string() {
    val tq = "\"\"\""
    Check("i am a cow", """ "i am a cow" """)
    Check( """ "hello" """.trim(), """ "\"hello\"" """.trim())

    Check("\n", """
    |$tq
    |$tq
    """.trimMargin())
    Check("\n\n\n", """
    |$tq
    |
    |
    |$tq
    """.trimMargin())
    val n = 1000
    Check(
      "\n" + "ABCDEFG".repeat(n),
      "\"\"\"\n" + "ABCDEFG".repeat(n) + "\"\"\""
    )
  }
}

class Misc {
  val Check = io.exoquery.pprint.Check(9999)
  val CheckNonGeneric = io.exoquery.pprint.Check(9999, showGenericForCollections = false)

  @Test fun nullTest() {
    Check.invoke<String?>(null, "null")
    Check(null as String?, "null")
    Check(listOf("look!", null as String?, "hi"), """List("look!", null, "hi")""")
  }
}

class Collections {
  val Check = io.exoquery.pprint.Check(9999)
  val CheckNonGeneric = io.exoquery.pprint.Check(9999, showGenericForCollections = false)

  @Serializable
  data class ExmapleValue(@Contextual val iter: Iterator<Int>)

  @Test
  fun iteratorTest() {
    Check(iteratorOf<String>(), "empty iterator", "<iterator>")
    Check(iteratorOf(1, 2, 3), "non-empty iterator", "<iterator>")
    Check(ExmapleValue(iteratorOf(1, 2, 3)), "ExmapleValue(non-empty iterator)", "ExmapleValue(<iterator>)")
  }

  @Test fun listTest() { Check(listOf(1, 2, 3), "List(1, 2, 3)") }
  @Test fun listTest2() { Check(listOf("1", "2", "3"), """List("1", "2", "3")""") }

  @Test fun arrayTest() { Check(arrayOf(1, 2, 3), "Array(1, 2, 3)") }

  @Test fun booleanArrayTest() { Check(booleanArrayOf(true, false, true), "BooleanArray(true, false, true)") }
  @Test fun byteArrayTest() { Check(byteArrayOf(1, 2, 3), "ByteArray(1, 2, 3)") }
  @Test fun charArrayTest() { Check(charArrayOf('a', 'b', 'c'), "CharArray('a', 'b', 'c')") }
  @Test fun doubleArrayTest() { Check(doubleArrayOf(1.0, 2.0, 3.0), "DoubleArray(1.0, 2.0, 3.0)", "DoubleArray(1, 2, 3)") }
  @Test fun floatArrayTest() { Check(floatArrayOf(1.0F, 2.0F, 3.0F), "FloatArray(1.0F, 2.0F, 3.0F)", "FloatArray(1, 2, 3)") }
  @Test fun intArrayTest() { Check(intArrayOf(1, 2, 3), "IntArray(1, 2, 3)") }
  @Test fun longArrayTest() { Check(longArrayOf(1L, 2L, 3L), "LongArray(1L, 2L, 3L)") }
  @Test fun shortArrayTest() { Check(shortArrayOf(1, 2, 3), "ShortArray(1, 2, 3)") }

  @Test fun bufferTest() { Check(mutableListOf("omg", "wtf", "bbq"), """List("omg", "wtf", "bbq")""") }

  @Test fun mutableSetTest() { Check(mutableSetOf("omg"), """Set("omg")""", """HashSet("omg")""", """LinkedHashSet("omg")""") }
  @Test fun collectionSetTest() { Check(setOf("omg"), """Set("omg")""", """LinkedHashSet("omg")""") }

  // Sorted set does not exist in KMP
  // test("SortedSet") { Check(
  //   sortedSetOf("1", "2", "3"),
  //   """TreeSet("1", "2", "3")""",
  //   """Set("1", "2", "3")"""
  // ) }

  @Test fun mutableMapTest() {
    Check(
      mutableMapOf("key" to "value"),
      """Map("key" -> "value")""",
      """HashMap("key" -> "value")""",
      """LinkedHashMap("key" -> "value")"""
    )
  }

  // Sorted map does not exist in KMP
  // test("SortedMap") { Check(
  //   sortedMapOf("key" to "v", "key2" to "v2"),
  //   """Map("key" -> "v", "key2" -> "v2")""",
  //   """SortedMap("key" -> "v", "key2" -> "v2")"""
  //   )
  // }

  @Test fun pairTest() {
    Check(
      "key" to "value",
      """Pair("key", "value")"""
    )
  }
}


