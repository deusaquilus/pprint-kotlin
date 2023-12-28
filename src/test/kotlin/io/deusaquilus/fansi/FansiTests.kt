package io.deusaquilus.fansi

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec


class FansiTests : FunSpec({

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

  test("parsing"){
    val r = Str(rgbOps).render()

    assert(Str(rgbOps).plainText == "+++---***///")
    assert(Str(rgb).plainText == "")
    assert(r == rgbOps + RTC)
    assert(Str(rgb).render() == "")
  }

  test("equality"){
    assert(Color.Red("foo") == Color.Red("foo"))
  }
  test("concat"){
    val concated = (Str(rgbOps) + Str(rgbOps)).render()
    val expected = rgbOps + RTC + rgbOps + RTC

    assert(concated == expected)
  }
  test("apply"){
    val concated = Str(Str(rgbOps), Str(rgbOps)).render()
    val expected = rgbOps + RTC + rgbOps + RTC

    assert(concated == expected)

    val concated2 = Str("hello", "world", "i am cow")
    val concated3 = Str("helloworld", "i am cow")
    assert(concated2 == concated3)

    val applied = Str("hello")
    assert(applied.plainText == "hello")
    assert(applied.getColors().all { it == 0.toLong() })
  }
  test("join"){
    val concated = Str.join(listOf(Str(rgbOps), Str(rgbOps))).render()
    val expected = rgbOps + RTC + rgbOps + RTC
    assert(concated == expected)

    val concated2 = Str.join(listOf(Str(rgbOps), Str("xyz"))).render()
    val expected2 = rgbOps + RTC + "xyz"
    assert(concated2 == expected2)

    val concated3 = Str.join(listOf(Str(rgbOps)), sep = "lol").render()
    val expected3 = rgbOps + RTC
    assert(concated3 == expected3)

    val concated4 = Str.join(listOf(Str(rgbOps), Str("xyz")), sep = "lol").render()
    val expected4 = rgbOps + RTC + "lol" + "xyz"
    assert(concated4 == expected4)

    val concated5 = Str.join(listOf(Str(rgbOps), Str("xyz"), Str(rgbOps)), sep = "lol").render()
    val expected5 = rgbOps + RTC + "lol" + "xyz" + "lol" + rgbOps + RTC
    assert(concated5 == expected5)
  }
  test("get"){
    val str = Str(rgbOps)
    val w = Attrs.Empty.transform(0)
    val r = Color.Red.transform(0)
    val g = Color.Green.transform(0)
    val b = Color.Blue.transform(0)

    assert(str.getChars().joinToString("") == "+++---***///")
    assert(str.getChar(0) == '+')
    assert(str.getChar(1) == '+')
    assert(str.getChar(2) == '+')
    assert(str.getChar(3) == '-')
    assert(str.getChar(4) == '-')
    assert(str.getChar(5) == '-')
    assert(str.getChar(6) == '*')
    assert(str.getChar(7) == '*')
    assert(str.getChar(8) == '*')
    assert(str.getChar(9) == '/')
    assert(str.getChar(10) == '/')
    assert(str.getChar(11) == '/')
    assert(str.getColors().toList() == listOf(w,w,w,r,r,r,g,g,g,b,b,b))
    assert(str.getColor(0) == w)
    assert(str.getColor(1) == w)
    assert(str.getColor(2) == w)
    assert(str.getColor(3) == r)
    assert(str.getColor(4) == r)
    assert(str.getColor(5) == r)
    assert(str.getColor(6) == g)
    assert(str.getColor(7) == g)
    assert(str.getColor(8) == g)
    assert(str.getColor(9) == b)
    assert(str.getColor(10) == b)
    assert(str.getColor(11) == b)
  }

  test("split"){
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
      assert(Pair(left, right) == Pair(expectedLeft, expectedRight))
    }
  }

  test("substring"){
    val substringed = Str(rgbOps).substring(4, 9).render()
    assert(substringed == "$R--$G***$RTC")

    val default = Str(rgbOps).render()

    val noOpSubstringed1 = Str(rgbOps).substring().render()
    assert(noOpSubstringed1 == default)

    val parsed = Str(rgbOps)
    val noOpSubstringed2 = parsed.substring(0, parsed.length).render()
    assert(noOpSubstringed2 == default)
  }

  context("overlay"){
    test("simple"){
      val overlayed = Str(rgbOps).overlay(Color.Yellow, 4, 7)
      val expected = "+++$R-$Y--*$G**$B///$RTC"
      assert(overlayed.render() == expected)
    }
    test("resetty"){
      val resetty = "+$RES++$R--$RES-$RES$G***$B///"
      val overlayed = Str(resetty).overlay(Color.Yellow, 4, 7).render()
      val expected = "+++$R-$Y--*$G**$B///$RTC"
      assert(overlayed == expected)
    }
    test("mixedResetUnderline"){
      val resetty = "+$RES++$R--$RES-$UND$G***$B///"
      val overlayed = Str(resetty).overlay(Color.Yellow, 4, 7).render().toList()
      val expected = "+++$R-$Y--$UND*$G**$B///$DCOL$DUND".toList()

      assert(overlayed == expected)
    }

    context("underlines"){
      val resetty = "$UND#$RES    $UND#$RES"
      test("underlineBug"){
        val overlayed = Str(resetty).overlay(Reversed.On, 0, 2).render()
        val expected = "$UND$REV#$DUND $DREV   $UND#$DUND"
        assert(overlayed == expected)
      }
      test("barelyOverlapping"){
        val overlayed = Str(resetty).overlay(Reversed.On, 0, 1).render()
        val expected = "$UND$REV#$DUND$DREV    $UND#$DUND"
        assert(overlayed == expected)
      }
      test("endOfLine"){
        val overlayed = Str(resetty).overlay(Reversed.On, 5, 6).render()
        val expected = "$UND#$DUND    $UND$REV#$DUND$DREV"
        assert(overlayed == expected)
      }
      test("overshoot"){
        shouldThrow<IllegalArgumentException> {
          Str(resetty).overlay(Reversed.On, 5, 10)
        }
      }
      test("empty"){
        val overlayed = Str(resetty).overlay(Reversed.On, 0, 0).render()
        val expected = "$UND#$DUND    $UND#$DUND"
        assert(overlayed == expected)
      }
      test("singleContent"){
        val overlayed = Str(resetty).overlay(Reversed.On, 2, 4).render()
        val expected = "$UND#$DUND $REV  $DREV $UND#$DUND"
        assert(overlayed == expected)
      }
    }

    test("overallAll"){
      //"+++$R---$G***$B///"
      val overlayed = Str(rgbOps).overlayAll(listOf(
        Triple(Color.Yellow, 4, 7),
        Triple(Underlined.On, 4, 7),
        Triple(Underlined.Off, 5, 6),
        Triple(Color.Blue, 7, 9)
      )).render()
      val expected = "+++$R-$Y$UND-$DUND-$UND*$B$DUND**///$DCOL"
      assert(overlayed == expected)
    }
  }

  context("attributes"){
    test("Reset/Underlined-On"){
      Console.RESET + Underlined.On
    }
    test("Reset/Reset/Underlined"){
      Console.RESET + (Underlined.On("Reset ") + Underlined.Off("Underlined"))
    }
    test("Reset/Bold-On"){
      Console.RESET + Bold.On
    }
    test("Reset/Bold-On/Bold-Off") {
      Console.RESET + (Bold.On("Reset ") + Bold.Off("Bold"))
    }
    test("Reset/Reversed-On"){
      Console.RESET + Reversed.On
    }
    test("Reset/Reversed-On/Reversed-Off"){
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

  // TODO Check this against real values coming from the equivalent fansi test
  test("colors") { tabulate(Color.all) }

  test("backgrounds") { tabulate(Back.all) }

  context("trueColor"){

    test("red") { println(Color.True(255,0,0)("Red")) }

    test("redhexa") { println(Color.True(0xFF0000)("redhexa")) }

    test("green") { println(Color.True(0,255,0)("green")) }

    test("greenhexa") { println(Color.True(0x00FF00)("greenhexa")) }

    test("blue") { println(Color.True(0,0,255)("blue")) }

    test("bluehaxe") { println(Color.True(0x0000FF)("bluehaxe")) }

    test("256 shades of gray") { square((0..255).map { i -> Color.True(i,i,i) } ) }

    test("trueColors") { tabulate((0 until 0xFFFFFF step 10000).map { i -> Color.True(i)  }) }

    test("trueBackgrounds") { tabulate((0 until 0xFFFFFF step 10000).map { i -> Back.True(i) }) }

    test("blackState") { assert (Color.lookupAttr(273 shl 3) == Color.True(0,0,0) ) }

    test("whitState") {  assert (Color.lookupAttr(16777488 shl 3) == Color.True(255,255,255) ) }

    test("redState") {  assert (Color.lookupAttr((0xFF0000 + 273) shl 3) == Color.True(255,0,0)) }

    test("lastFullState") { assert ( Color.lookupAttr(272 shl 3) == Color.Full[255] ) }

    context("parsing"){
      fun check(frag: Str) {
        val parsed = Str(frag.render())
        assert(parsed == frag)
        println(parsed)
      }
      test("Check 255,0,0") { check(Color.True(255, 0, 0)("lol")) }
      test("Check 1, 234, 56") { check(Color.True(1, 234, 56)("lol")) }
      test("Check 255, 255, 255") { check(Color.True(255, 255, 255)("lol")) }
      test("Check True(10000)") { check(Color.True(10000)("lol")) }
      test("Check 0..255") {
        for(i in 0..255) check(Color.True(i,i,i)("x"))
        println("")
      }
      test("Check 127, 126, 0 print") { check(
        ("#" + Color.True(127, 126, 0)("lol") + "omg" + Color.True(127, 126, 0)("wtf")).toStr()
      )}

      test("Check 0..255 Square") { square((0 ..255).map { i ->  Color.True(i,i,i)}) }

      context("failure"){
        context("tooLongToParse"){
          test("Too long: [38;2;0;0;256m") { shouldThrow<IllegalArgumentException> {
            Str("\u001b[38;2;0;0;256m").plainText.toList().map { it.code }
          }}
          test("Too long: [38;2;0;256;0m") {  shouldThrow<IllegalArgumentException> {
            Str("\u001b[38;2;0;256;0m").plainText.toList().map { it.code }
          }}
          test("Too long: [38;2;256;0;0m") {  shouldThrow<IllegalArgumentException> {
            Str("\u001b[38;2;256;0;0m").plainText.toList().map { it.code }
          }}
          test("Too long: [38;2;1111;0;0m") {  shouldThrow<IllegalArgumentException> {
            Str("\u001b[38;2;1111;0;0m").plainText.toList().map { it.code }
          }}
        }
        test("truncatedParsing"){
          val escape = Color.True(255, 0, 0).escape
          for (i in 1 until (escape.length - 1)) {
            shouldThrow < IllegalArgumentException > {
              Str(escape.dropLast(i))
            }
          }
        }
        context("args"){
          test("Should throw on: olor.True(256, 0, 0)") { shouldThrow<IllegalArgumentException>{ Color.True(256, 0, 0) } }
          test("Should throw on: Color.True(0, 256, 0") {  shouldThrow<IllegalArgumentException>{ Color.True(0, 256, 0) } }
          test("Should throw on: Color.True(0, 0, 256)") {  shouldThrow<IllegalArgumentException>{ Color.True(0, 0, 256) } }
          test("Should throw on: Color.True(-1, 0, 0)") {  shouldThrow<IllegalArgumentException>{ Color.True(-1, 0, 0) } }
          test("Should throw on: Color.True(0, -1, 0)") {  shouldThrow<IllegalArgumentException>{ Color.True(0, -1, 0) } }
          test("Should throw on: Color.True(0, 0, -1)") {  shouldThrow<IllegalArgumentException>{ Color.True(0, 0, -1) } }
        }
      }
    }

    context("emitAnsiCodes"){
      test("basic") {
        assert(Attrs.emitAnsiCodes(0, Color.Red.applyMask) == Console.RED)
        assert(Attrs.emitAnsiCodes(Color.Red.applyMask, 0) == Color.Reset.escape)
      }
      test("combo"){
        // One color stomps over the other
        val colorColor = Color.Red + Color.Blue
        assert(Attrs.emitAnsiCodes(0, colorColor.applyMask) == Console.BLUE)


        val colorBold = Color.Red + Bold.On
        assert(Attrs.emitAnsiCodes(0, colorBold.applyMask) == Console.RED + Console.BOLD)
        // unlike Colors and Underlined and Reversed, Bold needs a hard reset,
        assert(Attrs.emitAnsiCodes(colorBold.applyMask, 0) == Console.RESET)
      }
    }

    context("negative"){
      context("errorMode"){
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
          assert(thrownError.message?.contains(msg) ?: false)
          val thrownError2 = shouldThrow<IllegalArgumentException>{
            Str.Throw(s)
          }
          assert(thrownError2.message?.contains(msg) ?: false)
          // If I ask it to sanitize, the escape character is gone but the
          // rest of each escape sequence remains
          val sanitized = Str(s, errorMode = ErrorMode.Sanitize)
          assert(sanitized.plainText == ("Hello" + msg + "World"))
          val sanitized2 = Str.Sanitize(s)
          assert(sanitized2.plainText == ("Hello" + msg + "World"))

          // If I ask it to strip, everything is gone
          val stripped = Str(s, errorMode = ErrorMode.Strip)
          assert(stripped.plainText == "HelloWorld")
          val stripped2 = Str.Strip(s)
          assert(stripped2.plainText == "HelloWorld")
        }

        test("cursorUp") { check("Hello\u001b[2AWorld", "[2A") }
        test("cursorDown") { check("Hello\u001b[2BWorld", "[2B") }
        test("cursorForward") { check("Hello\u001b[2CWorld", "[2C") }
        test("cursorBack") { check("Hello\u001b[2DWorld", "[2D") }
        test("cursorNextLine") { check("Hello\u001b[2EWorld", "[2E") }
        test("cursorPrevLine") { check("Hello\u001b[2FWorld", "[2F") }
        test("cursorHorizontalAbs") { check("Hello\u001b[2GWorld", "[2G") }
        test("cursorPosition") { check("Hello\u001b[2;2HWorld", "[2;2H") }
        test("eraseDisplay") { check("Hello\u001b[2JWorld", "[2J") }
        test("eraseLine") { check("Hello\u001b[2KWorld", "[2K") }
        test("scrollUp") { check("Hello\u001b[2SWorld", "[2S") }
        test("scrollDown") { check("Hello\u001b[2TWorld", "[2T") }
        test("horizontalVerticalPos") { check("Hello\u001b[2;2fWorld", "[2;2f") }
        test("selectGraphicRendition") { check("Hello\u001b[2mWorld", "[2m") }
        test("auxPortOn") { check("Hello\u001b[5iWorld", "[5i") }
        test("auxPortOff") { check("Hello\u001b[4iWorld", "[4i") }
        test("deviceStatusReport") { check("Hello\u001b[6nWorld", "[6n") }
        test("saveCursor") { check("Hello\u001b[sWorld", "[") }
        test("restoreCursor") { check("Hello\u001b[uWorld", "[u") }
      }
      test("outOfBounds"){
        shouldThrow<IllegalArgumentException>{ Str("foo").splitAt(10) }
        shouldThrow<IllegalArgumentException>{ Str("foo").splitAt(4) }
        shouldThrow<IllegalArgumentException>{ Str("foo").splitAt(-1) }
        shouldThrow<IllegalArgumentException>{ Str("foo").substring(0, 4)}
        shouldThrow<IllegalArgumentException>{ Str("foo").substring(-1, 2)}
        shouldThrow<IllegalArgumentException>{ Str("foo").substring(2, 1)}
      }
    }

    context("multipleAttrs"){
      test("identicalMasksGetCollapsed"){
        val redRed = Color.Red + Color.Red
        assert(redRed.resetMask == Color.Red.resetMask)
        assert(redRed.applyMask == Color.Red.applyMask)
      }
      test("overlappingMasksGetReplaced"){
        val redBlue = Color.Red + Color.Blue
        assert(redBlue.resetMask == Color.Blue.resetMask)
        assert(redBlue.applyMask == Color.Blue.applyMask)
      }
      test("semiOverlappingMasks"){
        val resetRed = Attr.Reset + Color.Red
        val redReset = Color.Red + Attr.Reset

        assert(resetRed != Attr.Reset)
        assert(resetRed != Color.Red)
        assert(redReset == Attr.Reset)
        assert(redReset != Color.Red)
        assert(redReset != resetRed)
        assert(resetRed.resetMask == Attr.Reset.resetMask)
        assert(resetRed.applyMask == Color.Red.applyMask)

      }
      test("separateMasksGetCombined"){
        val redBold = Color.Red + Bold.On


        assert(redBold.resetMask == (Color.Red.resetMask or Bold.On.resetMask))
        assert(redBold.applyMask == (Color.Red.applyMask or Bold.On.applyMask))

      }
      test("applicationWorks"){
        val redBlueBold = Color.Red + Color.Blue + Bold.On
        val colored = redBlueBold("Hello World")
        val separatelyColored = Bold.On(Color.Blue(Color.Red("Hello World")))
        assert(colored.render() == separatelyColored.render())
      }
      test("equality"){
        assert(Color.Blue + Color.Red == Color.Red)
        assert(Color.Red == Color.Blue + Color.Red)
        assert(Bold.On + Color.Red != Color.Red)
        assert(Color.Red != Bold.On + Color.Red)
      }
    }
  }

  // TODO These work, check them against the Fansi perf results
//  context("perf"){
//    val input = "+++$R---$G***$B///".repeat(1000)
//
//    test("parsing"){
//      val start = System.currentTimeMillis()
//      var count = 0
//      while(System.currentTimeMillis() < start + 5000){
//        count += 1
//        Str(input)
//      }
//      val end = System.currentTimeMillis()
//      println("===== parsing: " + ((end - start).toDouble() / count) + "=====")
//    }
//    test("rendering"){
//
//      val start = System.currentTimeMillis()
//      var count = 0
//      val parsed = Str(input)
//      while(System.currentTimeMillis() < start + 5000){
//        count += 1
//        parsed.render()
//      }
//      val end = System.currentTimeMillis()
//      println("===== rendering: " + ((end - start).toDouble() / count) + "=====")
//    }
//    test("concat"){
//      val start = System.currentTimeMillis()
//      var count = 0
//      val fansiStr = Str(input)
//      while(System.currentTimeMillis() < start + 5000){
//        count += 1
//        fansiStr + fansiStr
//      }
//      val end = System.currentTimeMillis()
//      println("===== concat: " + ((end - start).toDouble() / count) + "=====")
//    }
//    test("splitAt"){
//      val start = System.currentTimeMillis()
//      var count = 0
//      val fansiStr = Str(input)
//      while(System.currentTimeMillis() < start + 5000){
//        count += 1
//        fansiStr.splitAt(count % fansiStr.length)
//      }
//      val end = System.currentTimeMillis()
//      println("===== splitAt: " + ((end - start).toDouble() / count) + "=====")
//    }
//    test("substring"){
//      val start = System.currentTimeMillis()
//      var count = 0
//      val fansiStr = Str(input)
//      while(System.currentTimeMillis() < start + 5000){
//        count += 1
//        val start = count % fansiStr.length
//        val end = count % (fansiStr.length - start) + start
//        fansiStr.substring(start, end)
//      }
//      val end = System.currentTimeMillis()
//      println("===== substring: " + ((end - start).toDouble() / count) + "=====")
//    }
//    test("overlay"){
//      val start = System.currentTimeMillis()
//      var count = 0
//      val fansiStr = Str(input)
//      val attrs =
//        Color.Red +
//          Color.Blue +
//          Bold.On +
//          Reversed.On +
//          Bold.Off +
//          Underlined.On
//
//      while(System.currentTimeMillis() < start + 5000){
//        count += 1
//        val start = count % fansiStr.length
//        val end = count % (fansiStr.length - start) + start
//        fansiStr.overlay(attrs, start, end)
//      }
//      val end = System.currentTimeMillis()
//      println("===== overlay: " + ((end - start).toDouble() / count) + "=====")
//    }
//  }

})