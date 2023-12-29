package io.deusaquilus.pprint

fun <T> iteratorOf(vararg args: T) =
  sequenceOf(*args).iterator()
