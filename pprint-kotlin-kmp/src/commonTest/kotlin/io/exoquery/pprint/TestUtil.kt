package io.exoquery.pprint

import kotlin.test.assertEquals

fun <T> iteratorOf(vararg args: T) =
  sequenceOf(*args).iterator()

infix fun <T> T.shouldBe(other: T) = assertEquals(this, other)