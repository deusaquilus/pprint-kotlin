package io.exoquery.kmp

import io.exoquery.fansi.Attrs
import io.exoquery.fansi.Color
import io.exoquery.kmp.pprint.PPrinter
import io.exoquery.pprint.PPrinterConfig

inline fun <reified T> pprint(
  value: T,
  defaultWidth: Int = 100,
  defaultHeight: Int = 500,
  defaultIndent: Int = 2,
  defaultEscapeUnicode: Boolean = false,
  defaultShowFieldNames: Boolean = true,
  colorLiteral: Attrs = Color.Green,
  colorApplyPrefix: Attrs = Color.Yellow,
  showGenericForCollections: Boolean = true
) = PPrinter.invoke<T>(
  PPrinterConfig(
    defaultWidth,
    defaultHeight,
    defaultIndent,
    defaultEscapeUnicode,
    defaultShowFieldNames,
    colorLiteral,
    colorApplyPrefix,
    showGenericForCollections
  )
).invoke(value)