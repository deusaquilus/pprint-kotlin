package io.exoquery

import io.exoquery.fansi.Attrs
import io.exoquery.fansi.Color
import io.exoquery.pprint.PPrinter

fun pprint(
  value: Any?,
  defaultWidth: Int = 100,
  defaultHeight: Int = 500,
  defaultIndent: Int = 2,
  defaultEscapeUnicode: Boolean = false,
  defaultShowFieldNames: Boolean = true,
  colorLiteral: Attrs = Color.Green,
  colorApplyPrefix: Attrs = Color.Yellow,
  showGenericForCollections: Boolean = true
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