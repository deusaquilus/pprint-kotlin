package io.exoquery.pprint

fun <T> iteratorOf(vararg args: T) =
  sequenceOf(*args).iterator()
