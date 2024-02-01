package io.exoquery.fansi

import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals

infix fun <T> T.shouldBe(other: T) = assertEquals(this, other)
infix fun <T> T.shouldNotBe(other: T) = assertNotEquals(this, other)

inline fun <reified T: Throwable> shouldThrow(block: () -> Unit): T =
  assertFailsWith(T::class, block)

