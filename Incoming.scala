package pprint

object Renderer{
  /**
    * Basically like mkString, but for nested iterators. Used whenever
    * you want to put stuff "in between" the elements of the larger
    * iterator
    */
  fun joinIter<T>(it0: Sequence<Sequence<T>>, joiner: () -> Sequence<T>) = {
    new Util.ConcatIterator(it0, () -> joiner)
  }

  val openParen = Str("(")
  val closeParen = Str(")")
  val commaSpace = Str(", ")
  val newLine = Str("\n")
  val commaNewLine = Str(",\n")
  private[this] val cachedIndents = Array.tabulate(64)(n -> Str(" " * n))
  fun indent(n: Int) = if (n < 64) cachedIndents(n) else Str(" " * n)
}
