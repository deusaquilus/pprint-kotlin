package io.exoquery.fansi

import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertFails
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import kotlin.time.TimeSource


open class FansiTestBase {
  // Alias a bunch of render()ed attributes to short names
  // to use in all our test cases
  val R = Color .Red.escape
  val G = Color.Green.escape
  val B = Color.Blue.escape
  val Y = Color.Yellow.escape
  val UND = Underlined.On.escape
  val DUND = Underlined.Off.escape
  val REV = Reversed.On.escape
  val DREV = Reversed.Off.escape
  val DCOL = Color.Reset.escape
  val RES = Attr.Reset.escape
  /**
   * ANSI escape sequence to reset text color
   */
  val RTC = Color.Reset.escape

  val rgbOps = "+++$R---$G***$B///"
  val rgb = "$R$G$B"
}

class BasicTests: FansiTestBase() {
  @Test
  fun parsing() {
    val r = Str(rgbOps).render()

    Str(rgbOps).plainText shouldBe "+++---***///"
    Str(rgb).plainText shouldBe ""
    r shouldBe rgbOps + RTC
    Str(rgb).render() shouldBe ""
  }

  @Test
  fun equality() {
    Color.Red("foo") shouldBe Color.Red("foo")
  }

  @Test
  fun concat() {
    val concated = (Str(rgbOps) + Str(rgbOps)).render()
    val expected = rgbOps + RTC + rgbOps + RTC

    concated shouldBe expected
  }

  @Test
  fun apply() {
    val concated = Str(rgbOps, rgbOps).render()
    val expected = rgbOps + RTC + rgbOps + RTC

    concated shouldBe expected

    val concated2 = Str("hello", "world", "i am cow")
    val concated3 = Str("helloworld", "i am cow")
    concated2 shouldBe concated3

    val applied = Str("hello")
    applied.plainText shouldBe "hello"
    applied.getColors().forEach { it shouldBe 0.toLong() }
  }

  @Test
  fun join() {
    val concated = Str.join(listOf(Str(rgbOps), Str(rgbOps))).render()
    val expected = rgbOps + RTC + rgbOps + RTC
    concated shouldBe expected

    val concated2 = Str.join(listOf(Str(rgbOps), Str("xyz"))).render()
    val expected2 = rgbOps + RTC + "xyz"
    concated2 shouldBe expected2

    val concated3 = Str.join(listOf(Str(rgbOps)), sep = "lol").render()
    val expected3 = rgbOps + RTC
    concated3 shouldBe expected3

    val concated4 = Str.join(listOf(Str(rgbOps), Str("xyz")), sep = "lol").render()
    val expected4 = rgbOps + RTC + "lol" + "xyz"
    concated4 shouldBe expected4

    val concated5 = Str.join(listOf(Str(rgbOps), Str("xyz"), Str(rgbOps)), sep = "lol").render()
    val expected5 = rgbOps + RTC + "lol" + "xyz" + "lol" + rgbOps + RTC
    concated5 shouldBe expected5
  }

  @Test
  fun get() {
    val str = Str(rgbOps)
    val w = Attrs.Empty.transform(0)
    val r = Color.Red.transform(0)
    val g = Color.Green.transform(0)
    val b = Color.Blue.transform(0)

    str.getChars().joinToString("") shouldBe "+++---***///"
    str.getChar(0) shouldBe '+'
    str.getChar(1) shouldBe '+'
    str.getChar(2) shouldBe '+'
    str.getChar(3) shouldBe '-'
    str.getChar(4) shouldBe '-'
    str.getChar(5) shouldBe '-'
    str.getChar(6) shouldBe '*'
    str.getChar(7) shouldBe '*'
    str.getChar(8) shouldBe '*'
    str.getChar(9) shouldBe '/'
    str.getChar(10) shouldBe '/'
    str.getChar(11) shouldBe '/'
    str.getColors().toList() shouldBe listOf(w,w,w,r,r,r,g,g,g,b,b,b)
    str.getColor(0) shouldBe w
    str.getColor(1) shouldBe w
    str.getColor(2) shouldBe w
    str.getColor(3) shouldBe r
    str.getColor(4) shouldBe r
    str.getColor(5) shouldBe r
    str.getColor(6) shouldBe g
    str.getColor(7) shouldBe g
    str.getColor(8) shouldBe g
    str.getColor(9) shouldBe b
    str.getColor(10) shouldBe b
    str.getColor(11) shouldBe b
  }

  @Test
  fun split() {
    val splits = listOf(
      // These are the standard series
      Triple(0,  "", "+++$R---$G***$B///$RTC"),
      Triple(1,  "+", "++$R---$G***$B///$RTC"),
      Triple(2,  "++", "+$R---$G***$B///$RTC"),
      Triple(3,  "+++", "$R---$G***$B///$RTC"),
      Triple(4,  "+++$R-$RTC", "$R--$G***$B///$RTC"),
      Triple(5,  "+++$R--$RTC", "$R-$G***$B///$RTC"),
      Triple(6,  "+++$R---$RTC", "$G***$B///$RTC"),
      Triple(7,  "+++$R---$G*$RTC", "$G**$B///$RTC"),
      Triple(8,  "+++$R---$G**$RTC", "$G*$B///$RTC"),
      Triple(9,  "+++$R---$G***$RTC", "$B///$RTC"),
      Triple(10, "+++$R---$G***$B/$RTC", "$B//$RTC"),
      Triple(11, "+++$R---$G***$B//$RTC", "$B/$RTC"),
      Triple(12, "+++$R---$G***$B///$RTC", "")
    )
    for((index, expectedLeft0, expectedRight0) in splits){
      val (splitLeft, splitRight) = Str(rgbOps).splitAt(index)
      val (expectedLeft, expectedRight) = Pair(expectedLeft0, expectedRight0)
      val left = splitLeft.render()
      val right = splitRight.render()
      Pair(left, right) shouldBe Pair(expectedLeft, expectedRight)
    }
  }

  @Test
  fun substring() {
    val substringed = Str(rgbOps).substring(4, 9).render()
    substringed shouldBe "$R--$G***$RTC"

    val default = Str(rgbOps).render()

    val noOpSubstringed1 = Str(rgbOps).substring().render()
    noOpSubstringed1 shouldBe default

    val parsed = Str(rgbOps)
    val noOpSubstringed2 = parsed.substring(0, parsed.length).render()
    noOpSubstringed2 shouldBe default
  }
}

class OverlayTests: FansiTestBase() {
  @Test fun simple() {
    val overlayed = Str(rgbOps).overlay(Color.Yellow, 4, 7)
    val expected = "+++$R-$Y--*$G**$B///$RTC"
    overlayed.render() shouldBe expected
  }
  @Test fun resetty() {
    val resetty = "+$RES++$R--$RES-$RES$G***$B///"
    val overlayed = Str(resetty).overlay(Color.Yellow, 4, 7).render()
    val expected = "+++$R-$Y--*$G**$B///$RTC"
    overlayed shouldBe expected
  }
  @Test fun mixedResetUnderline() {
    val resetty = "+$RES++$R--$RES-$UND$G***$B///"
    val overlayed = Str(resetty).overlay(Color.Yellow, 4, 7).render().toList()
    val expected = "+++$R-$Y--$UND*$G**$B///$DCOL$DUND".toList()
    overlayed shouldBe expected
  }

  @Test fun overallAll() {
    val overlayed = Str(rgbOps).overlayAll(listOf(
      Triple(Color.Yellow, 4, 7),
      Triple(Underlined.On, 4, 7),
      Triple(Underlined.Off, 5, 6),
      Triple(Color.Blue, 7, 9)
    )).render()
    val expected = "+++$R-$Y$UND-$DUND-$UND*$B$DUND**///$DCOL"
    overlayed shouldBe expected
  }
}

class UnderlinesTests: FansiTestBase() {
  val resetty = "$UND#$RES    $UND#$RES"
  @Test fun underlineBug() {
    val overlayed = Str(resetty).overlay(Reversed.On, 0, 2).render()
    val expected = "$UND$REV#$DUND $DREV   $UND#$DUND"
    overlayed shouldBe expected
  }
  @Test fun barelyOverlapping() {
    val overlayed = Str(resetty).overlay(Reversed.On, 0, 1).render()
    val expected = "$UND$REV#$DUND$DREV    $UND#$DUND"
    overlayed shouldBe expected
  }
  @Test fun endOfLine() {
    val overlayed = Str(resetty).overlay(Reversed.On, 5, 6).render()
    val expected = "$UND#$DUND    $UND$REV#$DUND$DREV"
    overlayed shouldBe expected
  }
  @Test fun overshoot() {
    shouldThrow<IllegalArgumentException> {
      Str(resetty).overlay(Reversed.On, 5, 10)
    }
  }
  @Test fun empty() {
    val overlayed = Str(resetty).overlay(Reversed.On, 0, 0).render()
    val expected = "$UND#$DUND    $UND#$DUND"
    overlayed shouldBe expected
  }
  @Test fun singleContent() {
    val overlayed = Str(resetty).overlay(Reversed.On, 2, 4).render()
    val expected = "$UND#$DUND $REV  $DREV $UND#$DUND"
    overlayed shouldBe expected
  }
}

class AttrsTests: FansiTestBase() {
  // Reset/Underlined-On
  @Test fun resetUnderlinedOn() {
    Console.RESET + Underlined.On
  }
  // Reset/Reset/Underlined
  @Test fun resetResetUnderlined() {
    Console.RESET + (Underlined.On("Reset ") + Underlined.Off("Underlined"))
  }
  // Reset/Bold-On
  @Test fun resetBoldOn() {
    Console.RESET + Bold.On
  }
  // Reset/Bold-On/Bold-Off
  @Test fun resetBoldOnBoldOff() {
    Console.RESET + (Bold.On("Reset ") + Bold.Off("Bold"))
  }
  // Reset/Reversed-On
  @Test fun resetReversedOn() {
    Console.RESET + Reversed.On
  }
  // Reset/Reversed-On/Reversed-Off
  @Test fun resetReversedOnReversedOff() {
    Console.RESET + (Reversed.On("Reset ") + Reversed.Off("Reversed"))
  }
}

fun tabulate(all: List<Attr>) {
  println(
    all.map { attr -> attr.toString() + " ".repeat(30 - attr.name.length) }
      .chunked(3)
      .map { it.joinToString("") }
      .joinToString("\n")
  )
}

fun square(all : List<Attr>) {
  println(
    all.map { attr -> (attr.escapeOpt ?: "") + "#" }
    .chunked(32)
    .map { it.joinToString("") }
    .joinToString("\n")
  )
}

class ColorTests: FansiTestBase() {
  @Test fun colors() {
    tabulate(Color.all)
  }
  @Test fun backgrounds() {
    tabulate(Back.all)
  }
  @Test fun trueColorRed() {
    println(Color.True(255, 0, 0)("Red"))
  }
  @Test fun trueColorRedHexa() {
    println(Color.True(0xFF0000)("redhexa"))
  }
  @Test fun trueColorGreen() {
    println(Color.True(0, 255, 0)("green"))
  }
  @Test fun trueColorGreenHexa() {
    println(Color.True(0x00FF00)("greenhexa"))
  }
  @Test fun trueColorBlue() {
    println(Color.True(0, 0, 255)("blue"))
  }
  @Test fun trueColorBlueHaxe() {
    println(Color.True(0x0000FF)("bluehaxe"))
  }
  @Test fun shadesOfGray() {
    square((0..255).map { i -> Color.True(i, i, i) })
  }
  @Test fun trueColors() {
    tabulate((0 until 0xFFFFFF step 10000).map { i -> Color.True(i) })
  }
  @Test fun trueBackgrounds() {
    tabulate((0 until 0xFFFFFF step 10000).map { i -> Back.True(i) })
  }
  @Test fun blackState() {
    Color.lookupAttr(273 shl 3) shouldBe Color.True(0, 0, 0)
  }
  @Test fun whiteState() {
    Color.lookupAttr(16777488 shl 3) shouldBe Color.True(255, 255, 255)
  }
  @Test fun redState() {
    Color.lookupAttr((0xFF0000 + 273) shl 3) shouldBe Color.True(255, 0, 0)
  }
  @Test fun lastFullState() {
    Color.lookupAttr(272 shl 3) shouldBe Color.Full[255]
  }
}

class ParsingTests: FansiTestBase() {
  fun check(frag: Str) {
    val parsed = Str(frag.render())
    parsed shouldBe frag
    println(parsed)
  }

  // Check 255,0,0
  @Test fun check255000() {
    val parsed = Str(Color.True(255, 0, 0)("lol").render())
    parsed shouldBe Color.True(255, 0, 0)("lol")
    println(parsed)
  }
  // Check 1, 234, 56
  @Test fun check123456() { check(Color.True(255, 0, 0)("lol")) }
  // Check 255, 255, 255
  @Test fun check255255255() { check(Color.True(255, 255, 255)("lol")) }
  // Check True(10000)
  @Test fun checkTrue10000() { check(Color.True(10000)("lol")) }
  // Check 0..255
  @Test fun check0255() {
    for(i in 0..255) check(Color.True(i,i,i)("x"))
    println("")
  }
  // Check 127, 126, 0 print
  @Test fun check1271260Print() { check(
    ("#" + Color.True(127, 126, 0)("lol") + "omg" + Color.True(127, 126, 0)("wtf")).toStr()
  ) }
  // Check 0..255 Square
  @Test fun check0255Square() { square((0 ..255).map { i ->  Color.True(i,i,i)}) }


}

class ParsingFailureTests: FansiTestBase() {
  @Test fun tooLongToParse() {
    shouldThrow<IllegalArgumentException> {
      Str("\u001b[38;2;0;0;256m").plainText.toList().map { it.code }
    }
    shouldThrow<IllegalArgumentException> {
      Str("\u001b[38;2;0;256;0m").plainText.toList().map { it.code }
    }
    shouldThrow<IllegalArgumentException> {
      Str("\u001b[38;2;256;0;0m").plainText.toList().map { it.code }
    }
    shouldThrow<IllegalArgumentException> {
      Str("\u001b[38;2;1111;0;0m").plainText.toList().map { it.code }
    }
  }
  @Test fun truncatedParsing() {
    val escape = Color.True(255, 0, 0).escape
    for (i in 1 until (escape.length - 1)) {
      shouldThrow<IllegalArgumentException> {
        Str(escape.dropLast(i))
      }
    }
  }
  @Test fun args() {
    shouldThrow<IllegalArgumentException> { Color.True(256, 0, 0) }
    shouldThrow<IllegalArgumentException> { Color.True(0, 256, 0) }
    shouldThrow<IllegalArgumentException> { Color.True(0, 0, 256) }
    shouldThrow<IllegalArgumentException> { Color.True(-1, 0, 0) }
    shouldThrow<IllegalArgumentException> { Color.True(0, -1, 0) }
    shouldThrow<IllegalArgumentException> { Color.True(0, 0, -1) }
  }
}

class EmitAnsiCodesTests: FansiTestBase() {
  @Test fun basic() {
    Attrs.emitAnsiCodes(0, Color.Red.applyMask) shouldBe Console.RED
    Attrs.emitAnsiCodes(Color.Red.applyMask, 0) shouldBe Color.Reset.escape
  }
  @Test fun combo() {
    // One color stomps over the other
    val colorColor = Color.Red + Color.Blue
    Attrs.emitAnsiCodes(0, colorColor.applyMask) shouldBe Console.BLUE

    val colorBold = Color.Red + Bold.On
    Attrs.emitAnsiCodes(0, colorBold.applyMask) shouldBe Console.RED + Console.BOLD
    // unlike Colors and Underlined and Reversed, Bold needs a hard reset,
    Attrs.emitAnsiCodes(colorBold.applyMask, 0) shouldBe Console.RESET
  }
}

class NegativeErrorMode: FansiTestBase() {
  // Make sure that Str throws on most common non-color
  // fansi terminal commands
  //
  // List of common non-color fansi terminal commands taken from
  // https://en.wikipedia.org/wiki/ANSI_escape_code#Non-CSI_codes

  fun check(s: String, msg: String) {
    // If I ask it to throw, it throws
    val thrownError = shouldThrow<IllegalArgumentException>{
      Str(s, errorMode = ErrorMode.Throw)
    }
    (thrownError.message?.contains(msg) ?: false) shouldBe true
    val thrownError2 = shouldThrow<IllegalArgumentException>{
      Str.Throw(s)
    }
    (thrownError2.message?.contains(msg) ?: false) shouldBe true
    // If I ask it to sanitize, the escape character is gone but the
    // rest of each escape sequence remains
    val sanitized = Str(s, errorMode = ErrorMode.Sanitize)
    // Note a shouldBe b tests are generally bad because in the case of failure
    // they don't print expected vs actual expressions. Should eventually replace all
    // of them with `shouldBe` but not doing that until the port of the library is complete.
    sanitized.plainText shouldBe ("Hello" + msg + "World")

    val sanitized2 = Str.Sanitize(s)
    sanitized2.plainText shouldBe ("Hello" + msg + "World")

    // If I ask it to strip, everything is gone
    val stripped = Str(s, errorMode = ErrorMode.Strip)
    stripped.plainText shouldBe "HelloWorld"
    val stripped2 = Str.Strip(s)
    stripped2.plainText shouldBe "HelloWorld"
  }

  @Test fun cursorUp() { check("Hello\u001b[2AWorld", "[2A") }
  @Test fun cursorDown() { check("Hello\u001b[2BWorld", "[2B") }
  @Test fun cursorForward() { check("Hello\u001b[2CWorld", "[2C") }
  @Test fun cursorBack() { check("Hello\u001b[2DWorld", "[2D") }
  @Test fun cursorNextLine() { check("Hello\u001b[2EWorld", "[2E") }
  @Test fun cursorPrevLine() { check("Hello\u001b[2FWorld", "[2F") }
  @Test fun cursorHorizontalAbs() { check("Hello\u001b[2GWorld", "[2G") }
  @Test fun cursorPosition() { check("Hello\u001b[2;2HWorld", "[2;2H") }
  @Test fun eraseDisplay() { check("Hello\u001b[2JWorld", "[2J") }
  @Test fun eraseLine() { check("Hello\u001b[2KWorld", "[2K") }
  @Test fun scrollUp() { check("Hello\u001b[2SWorld", "[2S") }
  @Test fun scrollDown() { check("Hello\u001b[2TWorld", "[2T") }
  @Test fun horizontalVerticalPos() { check("Hello\u001b[2;2fWorld", "[2;2f") }
  @Test fun selectGraphicRendition() { check("Hello\u001b[2mWorld", "[2m") }
  @Test fun auxPortOn() { check("Hello\u001b[5iWorld", "[5i") }
  @Test fun auxPortOff() { check("Hello\u001b[4iWorld", "[4i") }
  @Test fun deviceStatusReport() { check("Hello\u001b[6nWorld", "[6n") }
  @Test fun saveCursor() { check("Hello\u001b[sWorld", "[s") }
  @Test fun restoreCursor() { check("Hello\u001b[uWorld", "[u") }

  @Test fun outOfBounds() {
    // See comment by Str.split
    shouldThrow<IndexOutOfBoundsException>{ Str("foo").splitAt(10) }
    shouldThrow<IndexOutOfBoundsException>{ Str("foo").splitAt(4) }
    shouldThrow<IllegalArgumentException>{ Str("foo").splitAt(-1) }
    shouldThrow<IllegalArgumentException>{ Str("foo").substring(0, 4)}
    shouldThrow<IllegalArgumentException>{ Str("foo").substring(-1, 2)}
    shouldThrow<IllegalArgumentException>{ Str("foo").substring(2, 1)}
  }
}

class MultipleAttrsTests: FansiTestBase() {
  @Test fun identicalMasksGetCollapsed() {
    val redRed = Color.Red + Color.Red
    redRed.resetMask shouldBe Color.Red.resetMask
    redRed.applyMask shouldBe Color.Red.applyMask
  }
  @Test fun overlappingMasksGetReplaced() {
    val redBlue = Color.Red + Color.Blue
    redBlue.resetMask shouldBe Color.Blue.resetMask
    redBlue.applyMask shouldBe Color.Blue.applyMask
  }
  @Test fun semiOverlappingMasks() {
    val resetRed = Attr.Reset + Color.Red
    val redReset = Color.Red + Attr.Reset

    resetRed shouldNotBe Attr.Reset
    resetRed shouldNotBe Color.Red
    redReset shouldBe Attr.Reset
    redReset shouldNotBe Color.Red
    redReset shouldNotBe resetRed
    resetRed.resetMask shouldBe Attr.Reset.resetMask
    resetRed.applyMask shouldBe Color.Red.applyMask
  }
  @Test fun separateMasksGetCombined() {
    val redBold = Color.Red + Bold.On

    redBold.resetMask shouldBe (Color.Red.resetMask or Bold.On.resetMask)
    redBold.applyMask shouldBe (Color.Red.applyMask or Bold.On.applyMask)
  }
  @Test fun applicationWorks() {
    val redBlueBold = Color.Red + Color.Blue + Bold.On
    val colored = redBlueBold("Hello World")
    val separatelyColored = Bold.On(Color.Blue(Color.Red("Hello World")))
    colored.render() shouldBe separatelyColored.render()
  }
  @Test fun equality() {
    Color.Blue + Color.Red shouldBe Color.Red
    Color.Red shouldBe Color.Blue + Color.Red
    Bold.On + Color.Red shouldNotBe Color.Red
    Color.Red shouldNotBe Bold.On + Color.Red
  }
}

// Fansi performance tests to run on demand
class PerformanceTests: FansiTestBase() {
  val timeSource = TimeSource.Monotonic
  val input = "+++$R---$G***$B///".repeat(1000)

  @Ignore
  @Test
  fun parsing() {
    val start = timeSource.markNow()
    val finishAt = start + 5.seconds
    var count = 0
    while (finishAt.hasNotPassedNow()) {
      count += 1
      Str(input)
    }
    val end = timeSource.markNow()
    println("===== parsing: " + ((end - start).toDouble(DurationUnit.MILLISECONDS) / count) + "=====")
  }

  @Ignore
  @Test
  fun rendering() {
    val parsed = Str(input)
    val start = timeSource.markNow()
    val finishAt = start + 5.seconds
    var count = 0
    while (finishAt.hasNotPassedNow()) {
      count += 1
      parsed.render()
    }
    val end = timeSource.markNow()
    println("===== rendering: " + ((end - start).toDouble(DurationUnit.MILLISECONDS) / count) + "=====")
  }

  @Ignore
  @Test
  fun concat() {
    val fansiStr = Str(input)
    val start = timeSource.markNow()
    val finishAt = start + 5.seconds
    var count = 0
    while (finishAt.hasNotPassedNow()) {
      count += 1
      fansiStr + fansiStr
    }
    val end = timeSource.markNow()
    println("===== concat: " + ((end - start).toDouble(DurationUnit.MILLISECONDS) / count) + "=====")
  }

  @Ignore
  @Test
  fun splitAt() {
    val fansiStr = Str(input)
    val start = timeSource.markNow()
    val finishAt = start + 5.seconds
    var count = 0
    while (finishAt.hasNotPassedNow()) {
      count += 1
      fansiStr.splitAt(count % fansiStr.length)
    }
    val end = timeSource.markNow()
    println("===== splitAt: " + ((end - start).toDouble(DurationUnit.MILLISECONDS) / count) + "=====")
  }

  @Ignore
  @Test
  fun substring() {
    val fansiStr = Str(input)
    val start = timeSource.markNow()
    val finishAt = start + 5.seconds
    var count = 0
    while (finishAt.hasNotPassedNow()) {
      count += 1
      val startIdx = count % fansiStr.length
      val endIdx = count % (fansiStr.length - startIdx) + startIdx
      fansiStr.substring(startIdx, endIdx)
    }
    val end = timeSource.markNow()
    println("===== substring: " + ((end - start).toDouble(DurationUnit.MILLISECONDS) / count) + "=====")
  }

  @Ignore
  @Test
  fun overlay() {
    val fansiStr = Str(input)
    val attrs = Color.Red + Color.Blue + Bold.On + Reversed.On + Bold.Off + Underlined.On
    val start = timeSource.markNow()
    val finishAt = start + 5.seconds
    var count = 0
    while (finishAt.hasNotPassedNow()) {
      count += 1
      val startIdx = count % fansiStr.length
      val endIdx = count % (fansiStr.length - startIdx) + startIdx
      fansiStr.overlay(attrs, startIdx, endIdx)
    }
    val end = timeSource.markNow()
    println("===== overlay: " + ((end - start).toDouble(DurationUnit.MILLISECONDS) / count) + "=====")
  }
}
