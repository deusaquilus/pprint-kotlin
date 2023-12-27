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

  }

})