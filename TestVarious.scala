















      test("blackState") { assert (Color.lookupAttr(273 << 3) == Color.True(0,0,0) ) }

      test("whitState") {  assert (Color.lookupAttr(16777488 << 3) == Color.True(255,255,255) ) }

      test("redState") {  assert (Color.lookupAttr((0xFF0000 + 273) << 3) == Color.True(255,0,0)) }

      test("lastFullState") { assert ( Color.lookupAttr(272 << 3) == Color.Full(255)) }

      test("parsing"){
        fun check(frag: Str) = {
          val parsed = Str(frag.render())
          assert(parsed == frag)
          print(parsed)
        }
        test - check(Color.True(255, 0, 0)("lol"))
        test - check(Color.True(1, 234, 56)("lol"))
        test - check(Color.True(255, 255, 255)("lol"))
        test - check(Color.True(10000)("lol"))
        test{
          for(i <- 0 to 255) yield check(Color.True(i,i,i)("x"))
          println()
        }
        test - check(
          "#" + Color.True(127, 126, 0)("lol") + "omg" + Color.True(127, 126, 0)("wtf")
        )

        test - square(for(i <- 0 to 255) yield Color.True(i,i,i))


      }
      test("failure"){
        test("tooLongToParse"){
          test - intercept[IllegalArgumentException]{
            Str("\u001b[38;2;0;0;256m").plainText.toSeq.map(_.toInt)
          }
          test - intercept[IllegalArgumentException]{
            Str("\u001b[38;2;0;256;0m").plainText.toSeq.map(_.toInt)
          }
          test - intercept[IllegalArgumentException]{
            Str("\u001b[38;2;256;0;0m").plainText.toSeq.map(_.toInt)
          }
          test - intercept[IllegalArgumentException]{
            Str("\u001b[38;2;1111;0;0m").plainText.toSeq.map(_.toInt)
          }
        }
        test("truncatedParsing"){
          val escape = Color.True(255, 0, 0).escape
          for (i <- 1 until escape.length - 1)
          yield intercept[IllegalArgumentException] {
            Str(escape.dropRight(i))
          }
        }
        test("args"){
          test - intercept[IllegalArgumentException]{ Color.True(256, 0, 0) }
          test - intercept[IllegalArgumentException]{ Color.True(0, 256, 0) }
          test - intercept[IllegalArgumentException]{ Color.True(0, 0, 256) }
          test - intercept[IllegalArgumentException]{ Color.True(-1, 0, 0) }
          test - intercept[IllegalArgumentException]{ Color.True(0, -1, 0) }
          test - intercept[IllegalArgumentException]{ Color.True(0, 0, -1) }
        }
      }
    }

    test("emitAnsiCodes"){
      test("basic") { assert( }
        Attrs.emitAnsiCodes(0, Color.Red.applyMask) == Console.RED,
        Attrs.emitAnsiCodes(Color.Red.applyMask, 0) == Color.Reset.escape
      )
      test("combo"){
        // One color stomps over the other
        val colorColor = Color.Red ++ Color.Blue
        assert(Attrs.emitAnsiCodes(0, colorColor.applyMask) == Console.BLUE)


        val colorBold = Color.Red ++ Bold.On
        assert(Attrs.emitAnsiCodes(0, colorBold.applyMask) == Console.RED + Console.BOLD)
        // unlike Colors and Underlined and Reversed, Bold needs a hard reset,
        assert(Attrs.emitAnsiCodes(colorBold.applyMask, 0) == Console.RESET)
      }

    }

    test("negative"){
      test("errorMode"){
        // Make sure that Str throws on most common non-color
        // fansi terminal commands
        //
        // List of common non-color fansi terminal commands taken from
        // https://en.wikipedia.org/wiki/ANSI_escape_code#Non-CSI_codes

        fun check(s: String, msg: String) ={
          // If I ask it to throw, it throws
          val thrownError = intercept[IllegalArgumentException]{
            Str(s, errorMode = ErrorMode.Throw)
          }
          assert(thrownError.getMessage.contains(msg))
          val thrownError2 = intercept[IllegalArgumentException]{
            Str.Throw(s)
          }
          assert(thrownError2.getMessage.contains(msg))
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
        intercept[IllegalArgumentException]{ Str("foo").splitAt(10) }
        intercept[IllegalArgumentException]{ Str("foo").splitAt(4) }
        intercept[IllegalArgumentException]{ Str("foo").splitAt(-1) }
        intercept[IllegalArgumentException]{ Str("foo").substring(0, 4)}
        intercept[IllegalArgumentException]{ Str("foo").substring(-1, 2)}
        intercept[IllegalArgumentException]{ Str("foo").substring(2, 1)}
      }
    }
    test("multipleAttrs"){
      test("identicalMasksGetCollapsed"){
        val redRed = Color.Red ++ Color.Red
        assert(
          redRed.resetMask == Color.Red.resetMask,
          redRed.applyMask == Color.Red.applyMask
        )
      }
      test("overlappingMasksGetReplaced"){
        val redBlue = Color.Red ++ Color.Blue
        assert(
          redBlue.resetMask == Color.Blue.resetMask,
          redBlue.applyMask == Color.Blue.applyMask
        )
      }
      test("semiOverlappingMasks"){
        val resetRed = Attr.Reset ++ Color.Red
        val redReset = Color.Red ++ Attr.Reset
        assert(
          resetRed != Attr.Reset,
          resetRed != Color.Red,
          redReset == Attr.Reset,
          redReset != Color.Red,
          redReset != resetRed,
          resetRed.resetMask == Attr.Reset.resetMask,
          resetRed.applyMask == Color.Red.applyMask
        )
      }
      test("separateMasksGetCombined"){
        val redBold = Color.Red ++ Bold.On

        assert(
          redBold.resetMask == (Color.Red.resetMask | Bold.On.resetMask),
          redBold.applyMask == (Color.Red.applyMask | Bold.On.applyMask)
        )
      }
      test("applicationWorks"){
        val redBlueBold = Color.Red ++ Color.Blue ++ Bold.On
        val colored = redBlueBold("Hello World")
        val separatelyColored = Bold.On(Color.Blue(Color.Red("Hello World")))
        assert(colored.render() == separatelyColored.render())
      }
      test("equality"){
        assert(
          Color.Blue ++ Color.Red == Color.Red,
          Color.Red == Color.Blue ++ Color.Red,
          Bold.On ++ Color.Red != Color.Red,
          Color.Red != Bold.On ++ Color.Red
        )
      }
    }
//    test("perf"){
//      val input = "+++$R---$G***$B///" * 1000
//
//      test("parsing"){
//
//        val start = System.currentTimeMillis()
//        var count = 0
//        while(System.currentTimeMillis() < start + 5000){
//          count += 1
//          Str(input)
//        }
//        val end = System.currentTimeMillis()
//        count
//      }
//      test("rendering"){
//
//        val start = System.currentTimeMillis()
//        var count = 0
//        val parsed = Str(input)
//        while(System.currentTimeMillis() < start + 5000){
//          count += 1
//          parsed.render()
//        }
//        val end = System.currentTimeMillis()
//        count
//      }
//      test("concat"){
//        val start = System.currentTimeMillis()
//        var count = 0
//        val fansiStr = Str(input)
//        while(System.currentTimeMillis() < start + 5000){
//          count += 1
//          fansiStr ++ fansiStr
//        }
//        val end = System.currentTimeMillis()
//        count
//      }
//      test("splitAt"){
//        val start = System.currentTimeMillis()
//        var count = 0
//        val fansiStr = Str(input)
//        while(System.currentTimeMillis() < start + 5000){
//          count += 1
//          fansiStr.splitAt(count % fansiStr.length)
//        }
//        val end = System.currentTimeMillis()
//        count
//      }
//      test("substring"){
//        val start = System.currentTimeMillis()
//        var count = 0
//        val fansiStr = Str(input)
//        while(System.currentTimeMillis() < start + 5000){
//          count += 1
//          val start = count % fansiStr.length
//          val end = count % (fansiStr.length - start) + start
//          fansiStr.substring(start, end)
//        }
//        val end = System.currentTimeMillis()
//        count
//      }
//      test("overlay"){
//        val start = System.currentTimeMillis()
//        var count = 0
//        val fansiStr = Str(input)
//        val attrs =
//          Color.Red ++
//          Color.Blue ++
//          Bold.On ++
//          Reversed.On ++
//          Bold.Off ++
//          Underlined.On
//
//        while(System.currentTimeMillis() < start + 5000){
//          count += 1
//          val start = count % fansiStr.length
//          val end = count % (fansiStr.length - start) + start
//          fansiStr.overlay(attrs, start, end)
//        }
//        val end = System.currentTimeMillis()
//        count
//      }
//    }
  }