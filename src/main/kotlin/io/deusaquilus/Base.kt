package io.deusaquilus

import io.deusaquilus.fansi.Attrs
import io.deusaquilus.fansi.Color
import io.deusaquilus.pprint.PPrinter

fun pprint(
  value: Any?,
  defaultWidth: Int = 100,
  defaultHeight: Int = 500,
  defaultIndent: Int = 2,
  defaultEscapeUnicode: Boolean = false,
  defaultShowFieldNames: Boolean = true,
  colorLiteral: Attrs = Color.Green,
  colorApplyPrefix: Attrs = Color.Yellow,
  showGenericForCollections: Boolean = false
) = PPrinter(
  defaultWidth,
  defaultHeight,
  defaultIndent,
  defaultEscapeUnicode,
  defaultShowFieldNames,
  colorLiteral,
  colorApplyPrefix,
  showGenericForCollections
)(value)