package io.exoquery.pprint

import io.exoquery.fansi.Attrs
import io.exoquery.fansi.Color as FansiColor
import io.exoquery.fansi.Str

data class PrintSettings(
  val width: Int = DefaultWidth,
  val height: Int = DefaultHeight,
  val indent: Int = DefaultIndent,
  val initialOffset: Int = DefaultInitialOffset,
  val escapeUnicode: Boolean = DefaultEscapeUnicode,
  val showFieldNames: Boolean = DefaultShowFieldNames,
  val colorLiteral: Attrs = ColorLiteral,
  val colorApplyPrefix: Attrs = ColorApplyPrefix,
  val defaultShowGenericForCollections: Boolean = DefaultShowGenericForCollections
) {
  companion object {
    val DefaultWidth: Int = 100
    val DefaultHeight: Int = 500
    val DefaultIndent: Int = 2
    val DefaultInitialOffset: Int = 0
    val DefaultEscapeUnicode: Boolean = false
    val DefaultShowFieldNames: Boolean = true
    val ColorLiteral: Attrs = FansiColor.Green
    val ColorApplyPrefix: Attrs = FansiColor.Yellow
    val DefaultShowGenericForCollections: Boolean = true

    val Defaults =
      PrintSettings(
        DefaultWidth,
        DefaultHeight,
        DefaultIndent,
        DefaultInitialOffset,
        DefaultEscapeUnicode,
        DefaultShowFieldNames,
        ColorLiteral,
        ColorApplyPrefix,
        DefaultShowGenericForCollections
      )
  }
}

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
  val defaultWidth: Int = PrintSettings.DefaultWidth,
  val defaultHeight: Int = PrintSettings.DefaultHeight,
  val defaultIndent: Int = PrintSettings.DefaultIndent,
  val defaultEscapeUnicode: Boolean = PrintSettings.DefaultEscapeUnicode,
  val defaultShowFieldNames: Boolean = PrintSettings.DefaultShowFieldNames,
  val colorLiteral: Attrs = PrintSettings.ColorLiteral,
  val colorApplyPrefix: Attrs = PrintSettings.ColorApplyPrefix,
  override val showGenericForCollections: Boolean = PrintSettings.DefaultShowGenericForCollections
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
    // important! this needs to be iterated more than once in the Truncated class
    Str.joinSafe(
      this.tokenize(
        x,
        width,
        height,
        indent,
        initialOffset,
        escapeUnicode = escapeUnicode,
        showFieldNames = showFieldNames
      )
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
    return TreeRenderer.makeStr(tree, PrintSettings(width, height, indent, initialOffset, escapeUnicode, showFieldNames))
  }

  companion object {
    val Color = PPrinter()
    val BlackWhite = PPrinter(
      colorLiteral = Attrs.Empty,
      colorApplyPrefix = Attrs.Empty
    )
  }
}

object TreeRenderer {
  fun makeStr(tree: Tree, settings: PrintSettings): Iterator<Str> {
    // Render the `Any` into a stream of tokens, properly indented and wrapped
    // at the given width
    val renderer = Renderer(settings.width, settings.colorApplyPrefix, settings.colorLiteral, settings.indent)
    val rendered = renderer.rec(tree, settings.initialOffset, 0).iter
    // Truncate the output stream once it's wrapped-at-width height goes
    // beyond the desired height
    val truncated = Truncated(rendered, settings.width, settings.height)
    return truncated
  }
}