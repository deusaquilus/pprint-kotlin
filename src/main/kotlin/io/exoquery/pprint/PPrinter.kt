package io.exoquery.pprint

import io.exoquery.fansi.Attrs
import io.exoquery.fansi.Color
import io.exoquery.fansi.Str

/**
  *
  * @param defaultWidth How wide to allow a pretty-printed value to become
  *                     before wrapping
  * @param defaultHeight How tall to allow the pretty-printed output to become
  *                      before truncated it with a `...`
  * @param defaultIndent How many spaces to indent each nested [[Tree.Apply]] by
  * @param colorLiteral What color to assign to literals like `"lol"` or 31337
  * @param colorApplyPrefix What color to assign to `Foo` in `Foo(bar, baz)`
  * @param additionalHandlers Provide this to override how certain types are
  *                           pretty-printed at runtime
  */
data class PPrinter(
  val defaultWidth: Int = 100,
  val defaultHeight: Int = 500,
  val defaultIndent: Int = 2,
  val defaultEscapeUnicode: Boolean = false,
  val defaultShowFieldNames: Boolean = true,
  val colorLiteral: Attrs = Color.Green,
  val colorApplyPrefix: Attrs = Color.Yellow,
  override val showGenericForCollections: Boolean = false
): Walker() {

  /**
    * Converts an [[Any]] into a large colored `Str`
    */
  operator fun invoke(
    x: Any?,
    width: Int = defaultWidth,
    height: Int = defaultHeight,
    indent: Int = defaultIndent,
    initialOffset: Int = 0,
    escapeUnicode: Boolean = defaultEscapeUnicode,
    showFieldNames: Boolean = defaultShowFieldNames
  ): Str =
    Str.join(
      this.tokenize(
        x,
        width,
        height,
        indent,
        initialOffset,
        escapeUnicode = escapeUnicode,
        showFieldNames = showFieldNames
      ).asSequence().toList() // important! this needs to be iterated more than once in the Truncated class
    )


  /**
    * Converts an [[Any]] into a large colored `Str`
    */
  fun <T> pprintln(x: T,
                  width: Int = defaultWidth,
                  height: Int = defaultHeight,
                  indent: Int = defaultIndent,
                  initialOffset: Int = 0,
                  escapeUnicode: Boolean = defaultEscapeUnicode,
                  showFieldNames: Boolean = defaultShowFieldNames): Unit {
    tokenize(
      x as Any,
      width,
      height,
      indent,
      initialOffset,
      escapeUnicode = escapeUnicode,
      showFieldNames = showFieldNames
    ).forEach { print(it) }
    println()
  }

  /**
    * Converts an [[Any]] into an iterator of colored chunks, wrapped at a
    * certain width and truncated at a certain height
    */
  fun tokenize(
    x: Any?,
    width: Int = defaultWidth,
    height: Int = defaultHeight,
    indent: Int = defaultIndent,
    initialOffset: Int = 0,
    escapeUnicode: Boolean = defaultEscapeUnicode,
    showFieldNames: Boolean = defaultShowFieldNames): Iterator<Str> {

    // The three stages within the pretty-printing process:

    // Convert the Any into a lazy Tree of `Apply`, `Infix` and `Lazy`/`Strict` literals
    val tree = this.treeify(x, escapeUnicode, showFieldNames)
    // Render the `Any` into a stream of tokens, properly indented and wrapped
    // at the given width
    val renderer = Renderer(width, colorApplyPrefix, colorLiteral, indent)
    val rendered = renderer.rec(tree, initialOffset, 0).iter
    // Truncate the output stream once it's wrapped-at-width height goes
    // beyond the desired height
    val truncated = Truncated(rendered, width, height)
    return truncated
  }

  companion object {
    //object Color: PPrinter
    val BlackWhite = PPrinter(
      colorLiteral = Attrs.Empty,
      colorApplyPrefix = Attrs.Empty
    )
  }
}

