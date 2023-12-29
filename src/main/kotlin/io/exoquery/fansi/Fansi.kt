package io.exoquery.fansi

import java.util.Arrays

typealias State = Long

fun CharSequence.toStr() = Str(this)

data class Name(val value: String) {
  companion object {
    val Unknown = Name("???")
  }
}

/**
 * Encapsulates a string with associated ANSI colors and text decorations.
 *
 * This is your primary data-type when you are dealing with colored fansi
 * strings.
 *
 * Contains some basic string methods, as well as some ansi methods to e.g.
 * apply particular colors or other decorations to particular sections of
 * the [[fansi.Str]]. [[render]] flattens it out into a `java.lang.String`
 * with all the colors present as ANSI escapes.
 *
 * Avoids using Scala collections operations in favor of util.Arrays,
 * giving 20% (on `++`) to >1000% (on `splitAt`, `subString`
 * and `Str.parse`) speedups
 */
class Str private constructor(private val chars: Array<Char>, private val colors: Array<State>) {
  //require(chars.length == colors.length)

  private fun require(condition: Boolean) {
    if (!condition) throw IllegalArgumentException()
  }
  private fun require(condition: Boolean, message: () -> String) {
    if (!condition) throw IllegalArgumentException(message())
  }

  private fun require(condition: Boolean, message: String) {
    if (!condition) throw IllegalArgumentException(message)
  }

  override fun hashCode() = java.util.Arrays.hashCode(chars) + java.util.Arrays.hashCode(colors)

  override fun equals(other: Any?) =
    when (other) {
      is Str ->
        java.util.Arrays.equals(chars, other.chars) && Arrays.equals(colors, other.colors)
      else -> false
    }

  /**
   * Concatenates two [[fansi.Str]]s, preserving the colors in each one and
   * avoiding any interference between them
   */
  operator fun plus(other: Str): Str {
    val newChars = Arrays.copyOf(chars, chars.size + other.chars.size)
    val newColors = Arrays.copyOf(colors, colors.size + other.colors.size)
    System.arraycopy(other.chars, 0, newChars, chars.size, other.chars.size)
    System.arraycopy(other.colors, 0, newColors, colors.size, other.colors.size)
    return Str(newChars, newColors)
  }

  /**
   * Splits an [[fansi.Str]] into two sub-strings, preserving the colors in
   * each one.
   *
   * @param index the plain-text index of the point within the [[fansi.Str]]
   *              you want to use to split it.
   */
  fun splitAt(index: Int) =
    Pair(
      Str(Arrays.copyOfRange(chars, 0, index), Arrays.copyOfRange(colors, 0, index)),
      Str(Arrays.copyOfRange(chars, index, chars.size), Arrays.copyOfRange(colors, index, colors.size))
    )


  /**
   * Returns an [[fansi.Str]] which is a substring of this string,
   * and has the same colors as the original section of this string
   * did
   */
  fun substring(start: Int = 0, end: Int = chars.size): Str {
    require(start >= 0 && end <= chars.size) {
      "substring start parameter $start must be >= 0 and end parameter $end must be <= ${chars.size}"
    }
    require(end >= start) {
      "substring end parameter $end must be >= start parameter $start"
    }
    return Str(Arrays.copyOfRange(chars, start, end), Arrays.copyOfRange(colors, start, end))
  }

  /**
   * The plain-text length of this [[fansi.Str]], in UTF-16 characters (same
   * as `.length` on a `java.lang.String`). If you want fancy UTF-8 lengths,
   * use `.plainText`
   */
  val length get() = chars.size


  override fun toString() = render()

  /**
   * The plain-text `java.lang.String` represented by this [[fansi.Str]],
   * without all the fansi colors or other decorations
   */
  val plainText: String by lazy { String(chars.toCharArray()) }

    /**
    * Returns a copy of the colors array backing this `fansi.Str`, in case
    * you want to use it to
    */
  fun getColors() = colors.clone()

  /**
    * Retrieve the color of this string at the given character index
    */
  fun getColor(i: Int) = colors[i]
  /**
    * Returns a copy of the character array backing this `fansi.Str`, in case
    * you want to use it to
    */
  fun getChars() = chars.clone()
  /**
    * Retrieve the character of this string at the given character index
    */
  fun getChar(i: Int) = chars[i]

  /**
    * Converts this [[fansi.Str]] into a `java.lang.String`, including all
    * the fancy fansi colors or decorations as fansi escapes embedded within
    * the string. "Terminates" colors at the right-most end of the resultant
    * `java.lang.String`, making it safe to concat-with or embed-inside other
    * `java.lang.String` without worrying about fansi colors leaking out of it.
    */
  fun render(): String {
    // Pre-size StringBuilder with approximate size (ansi colors tend
    // to be about 5 chars long) to avoid re-allocations during growth
    val output = StringBuilder(chars.size + colors.size * 5)

    var currentState: State = 0

        // Make a local array copy of the immutable Vector, for maximum performance
    // since the Vector is small and we'll be looking it up over & over & over
    val categoryArray = Attr.categories.toTypedArray()

    var i = 0
    while(i < colors.size){
      // Emit ANSI escapes to change colors where necessary
      // fast-path optimization to check for integer equality first before
      // going through the whole `enableDiff` rigmarole
      if (colors[i] != currentState) {
        Attrs.emitAnsiCodes0(currentState, colors[i], output, categoryArray)
        currentState = colors[i]
      }
      output.append(chars[i])
      i += 1
    }

    // Cap off the left-hand-side of the rendered string with any ansi escape
    // codes necessary to rest the state to 0
    Attrs.emitAnsiCodes0(currentState, 0, output, categoryArray)

    return output.toString()
  }

  fun overlay(attrs: Attrs, start: Int = 0, end: Int = length) =
    overlayAll(listOf(Triple(attrs, start, end)))

  fun overlayAll(attrs: List<Triple<Attrs, Int, Int>>): Str {
    val newColors = colors.clone()
    for ((attrs, start, end) in attrs) {
      require(end >= start) {
        "overlay end parameter $end must be >= start parameter $start"
      }
      require(start >= 0, "start:$start must be greater than or equal to 0")
      require(
        end <= colors.size,
        "end:$end must be less than or equal to length:${colors.size}"
      )
      var i = start
      while (i < end) {
        newColors[i] = attrs.transform(newColors[i])
        i += 1
      }
    }
    return Str(chars, newColors)
  }


  companion object {
    val ansiRegex = "(\u009b|\u001b\\[)[0-?]*[ -\\/]*[@-~]".toRegex().toPattern()

    /** Shorthand constructor with ErrorMode.Sanitize */
    fun Sanitize(raw: CharSequence) = invoke(raw, ErrorMode.Sanitize)

    /** Shorthand constructor with ErrorMode.Strip */
    fun Strip(raw: CharSequence) = invoke(raw, ErrorMode.Strip)

    /** Shorthand constructor with ErrorMode.Throw */
    fun Throw(raw: CharSequence) = invoke(raw, ErrorMode.Throw)
    /**
     * Creates an [[fansi.Str]] from a non-fansi `java.lang.String` or other
     * `CharSequence`.
     *
     * Note that this method is implicit, meaning you can pass in a
     * `java.lang.String` anywhere an `fansi.Str` is required and it will be
     * automatically parsed and converted for you.
     *
     * @param errorMode Used to control what kind of behavior you get if the
     *                  input `CharSequence` contains an Ansi escape not
     *                  recognized by Fansi as a valid color.
     */
    operator fun invoke(raw: CharSequence, errorMode: ErrorMode = ErrorMode.Throw): Str {
      val chars = Array<Char>(raw.length, {Char(0)})
      val colors = Array<State>(raw.length, {0})

      var currentColor: Long = 0
      var sourceIndex = 0
      var destIndex = 0
      var length = raw.length

      while(sourceIndex < length){
        val char = raw[sourceIndex]
        if (char == '\u001b' || char == '\u009b') {
          val escapeStartSourceIndex = sourceIndex
          val tuple = ParseMap.query(raw, escapeStartSourceIndex)
          when {
            tuple == null -> sourceIndex = errorMode.handle(sourceIndex, raw)
            else -> {
              val (newIndex, escape) = tuple
              when {
                escape is Either.Left -> { // seems to have the right template type info as long as we don't generalize it by doing Either.Left<*, *>
                  val color = escape.value
                  currentColor = color.transform(currentColor)
                  sourceIndex += newIndex
                }
                escape is Either.Right -> {
                  val category = escape.value
                  // Gross manual char-by-char parsing of the remainder
                  // of the True-color escape, to maximize performance
                  sourceIndex += newIndex
                  fun isDigit(index: Int) =
                    index < raw.length && raw[index] >= '0' && raw[index] <= '9'

                  fun checkChar(index: Int, char: Char) =
                    index < raw.length && raw[index] == char

                  fun fail() {
                    sourceIndex = errorMode.handle(escapeStartSourceIndex, raw)
                  }

                  fun getNumber(): Int {
                    var value = 0
                    var count = 0
                    while (isDigit(sourceIndex) && count < 3) {
                      value = value * 10 + (raw[sourceIndex] - '0').toInt()
                      sourceIndex += 1
                      count += 1
                    }
                    return value
                  }
                  if (!isDigit(sourceIndex)) fail()
                  else {
                    val r = getNumber()
                    if (!checkChar(sourceIndex, ';') || !isDigit(sourceIndex + 1)) fail()
                    else {
                      sourceIndex += 1
                      val g = getNumber()
                      if (!checkChar(sourceIndex, ';') || !isDigit(sourceIndex + 1)) fail()
                      else {
                        sourceIndex += 1
                        val b = getNumber()
                        if (!checkChar(sourceIndex, 'm')) fail()
                        else {
                          sourceIndex += 1
                          // Manually perform the `transform` for perf to avoid
                          // calling `True` which instantiates/allocaties an `Attr`
                          if(!(0 <= r && r < 256 && 0 <= g && g < 256 && 0 <= b && b < 256)) fail()
                          else{
                            currentColor = {
                              (currentColor and category.mask.toLong().inv()) or
                                ((273 + category.trueIndex(r, g, b)) shl category.offset).toLong()
                            }()
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        } else {
          colors[destIndex] = currentColor
          chars[destIndex] = char
          sourceIndex += 1
          destIndex += 1
        }
      }

      return Str(
        Arrays.copyOfRange(chars, 0, destIndex),
        Arrays.copyOfRange(colors, 0, destIndex)
      )
    }

    fun fromArrays(chars: Array<Char>, colors: Array<State>) =
      Str(chars.clone(), colors.clone())

    operator fun invoke(vararg args: Str): Str =
      join(args.toList())

    // Need to be careful here about the type of args, since we can easily go into an infinite loop
    operator fun invoke(vararg args: String): Str =
      // If there's only one argument, call the main invoke function
      if (args.size == 1) invoke(args[0] as CharSequence)
      // Otherwise invoke one by one and join them
      else join(args.map { invoke(it as CharSequence) })

    fun join(args: Iterable<Str>, sep: String): Str =
      join(args, sep.toStr())

    fun join(args: Iterable<Str>): Str =
      join(args, Str(""))

    fun join(args: Iterator<Str>, sep: String): Str =
      join(args.asSequence().toList(), sep.toStr())

    fun join(args: Iterator<Str>): Str =
      join(args.asSequence().toList(), Str(""))

    fun join(args: Iterable<Str>, sep: Str): Str {
      val length = args.map { it.length + sep.length }.sum() - sep.length
      val chars = Array<Char>(length, {Char(0)})
      val colors = Array<State>(length, {0})
      var j = 0
      for (arg in args){

        if (j != 0){
          var k = 0
          while (k < sep.length){
            chars[j] = sep.getChar(k)
            colors[j] = sep.getColors()[k]
            j += 1
            k += 1
          }
        }
        var i = 0
        while (i < arg.length){
          chars[j] = arg.getChar(i)
          colors[j] = arg.getColor(i)
          i += 1
          j += 1
        }
      }
      return fromArrays(chars, colors)
    }

    val ParseMap: Trie<Either<out Attr, out ColorCategory>> by lazy {
      val pairs =
        Attr.categories.flatMap { cat ->
          cat.all.flatMap { color ->
            color.escapeOpt.nullableAsList().map { str ->
              Pair(str, Either.Left<Attr, Nothing>(color))
            }
          }
        }

      val reset: List<Pair<String, Either.Left<Attr, Nothing>>> = listOf(Console.RESET to Either.Left<Attr, Nothing>(Attr.Reset))

      val trueColors: List<Pair<String, Either.Right<Nothing, ColorCategory>>>  = listOf(
        "\u001b[38;2;" to Either.Right<Nothing, ColorCategory>(Color),
        "\u001b[48;2;" to Either.Right<Nothing, ColorCategory>(Back)
      )

      Trie(pairs + reset + trueColors)
    }
  }
}


fun <T> T?.nullableAsList(): List<T> = if (this == null) emptyList() else listOf(this)

sealed interface Either<L, R> {
  data class Right<L, R>(val value: R) : Either<L, R>
  data class Left<L, R>(val value: L) : Either<L, R>
}



sealed interface ErrorMode {
  /**
   * Given an unknown Ansi escape was found at `sourceIndex` inside your
   * `raw: CharSequence`, what index should you resume parsing at?
   */
  fun handle(sourceIndex: Int, raw: CharSequence): Int

  object Throw : ErrorMode {
    override fun handle(sourceIndex: Int, raw: CharSequence): Int {
      val matcher = Str.ansiRegex.matcher(raw)
      val detail =
        if (!matcher.find(sourceIndex)) ""
        else {
          val end = matcher.end()
          raw.subSequence(sourceIndex + 1, end)
        }

      throw IllegalArgumentException(
        "Unknown ansi-escape$detail at index $sourceIndex " +
        "inside string cannot be parsed into an fansi.Str"
      )
    }
  }

  object Sanitize : ErrorMode {
    override fun handle(sourceIndex: Int, raw: CharSequence) = sourceIndex + 1
  }

  object Strip : ErrorMode {
    override fun handle(sourceIndex: Int, raw: CharSequence): Int {
      val matcher = Str.ansiRegex.matcher(raw)
      matcher.find(sourceIndex) // what is the purpose of this?
      return matcher.end()
    }
  }
}




/**
 * Represents one or more [[fansi.Attr]]s, that can be passed around
 * as a set or combined with other sets of [[fansi.Attr]]s.
 *
 * Note that a single [[Attr]] is a subclass of [[Attrs]]. If you want to
 * know if this contains multiple [[Attr]]s, you should check for
 * [[Attrs.Multiple]].
 */
sealed interface Attrs{

  /**
   * Apply these [[Attrs]] to the given [[fansi.Str]], making it take effect
   * across the entire length of that string.
   */
  operator fun invoke(s: Str) = s.overlay(this, 0, s.length)
  operator fun invoke(s: String) = s.toStr().overlay(this, 0, s.length)

  /**
   * Which bits of the [[Str.State]] integer these [[Attrs]] will
   * override when it is applied
   */
  val resetMask: Long

    /**
     * Which bits of the [[Str.State]] integer these [[Attrs]] will
     * set to `1` when it is applied
     */
    val applyMask: Long

    /**
     * Apply the current [[Attrs]] to the [[Str.State]] integer,
     * modifying it to represent the state after all changes have taken
     * effect
     */
    fun transform(state: State) = (state and resetMask.inv()) or applyMask

  /**
   * Combine this [[fansi.Attrs]] with other [[fansi.Attrs]]s, returning one
   * which when applied is equivalent to applying this one and then the `other`
   * one in series.
   */
  operator fun plus(other: Attrs): Attrs

  class Multiple(override val resetMask: Long, override val applyMask: Long, vararg val attrs: Attr): Attrs {
    init {
      assert(attrs.size != 1)
    }

    override fun hashCode() = attrs.hashCode()
    override fun equals(other: Any?): Boolean {
      val lhs = this
      val rhs = other
      @Suppress("USELESS_IS_CHECK")
      return when {
        // (not a possible case) lhs is Attr && rhs is Attr -> lhs === rhs
        // (not a possible case) lhs is Attr && rhs is Attrs.Multiple && rhs.attrs.size == 1 -> lhs === rhs.attrs[0]
        lhs is Attrs.Multiple && rhs is Attr && lhs.attrs.size == 1 -> lhs.attrs[0] === rhs
        lhs is Attrs.Multiple && rhs is Attrs.Multiple -> lhs.attrs === rhs.attrs
        else -> false
      }
    }

    override fun toString() = "Attrs(${attrs.joinToString(",")})"

    override operator fun plus(other: Attrs) = Attrs(*(attrs.toList() + toSeq(other)).toTypedArray())
  }

  companion object {
    val Empty = Attrs()

    fun emitAnsiCodes(currentState: State, nextState: State): String {
      val output = StringBuilder()
      val categoryArray = Attr.categories.toTypedArray()
      emitAnsiCodes0(currentState, nextState, output, categoryArray)
      return output.toString()
    }

    fun emitAnsiCodes0(currentState: State, nextState: State, output: StringBuilder, categoryArray: Array<Category>) {
      if (currentState != nextState){
        val hardOffMask = Bold.mask.toLong()
        // Any of these transitions from 1 to 0 within the hardOffMask
        // categories cannot be done with a single ansi escape, and need
        // you to emit a RESET followed by re-building whatever ansi state
        // you previous had from scratch
        val currentState2: Long =
          if ((currentState and nextState.inv() and hardOffMask) != 0.toLong()){
            output.append(Console.RESET)
            0
          }else {
            currentState
          }

        var categoryIndex = 0
        while(categoryIndex < categoryArray.size){
          val cat = categoryArray[categoryIndex]
          if ((cat.mask.toLong() and currentState2) != (cat.mask.toLong() and nextState)){
            val escape = cat.lookupEscape(nextState and cat.mask.toLong())
            output.append(escape)
          }
          categoryIndex += 1
        }
      }
    }

    operator fun invoke(vararg attrs: Attr): Attrs {
      var output: List<Attr> = listOf<Attr>()
      var resetMask: Long = 0
      var applyMask: Long = 0
      // Walk the list of attributes backwards, and aggregate only those whose
      // `resetMask` is not going to get totally covered by the union of all
      // `resetMask`s that come after it.
      //
      // Simultaneously build up the `applyMask`, which is the `applyMask` of
      // all aggregated `attr`s whose own `applyMask` is not totally covered by
      // the union of all `resetMask`s that come after.
      for(attr in attrs.reversed().iterator()){
          if ((attr.resetMask and resetMask.inv()) != 0.toLong()){
          if ((attr.applyMask and resetMask) == 0.toLong()) applyMask = applyMask or attr.applyMask
          resetMask = resetMask or attr.resetMask
          output = listOf(attr) + output
        }
      }

      return if (output.size == 1) output.first()
      else Multiple(resetMask, applyMask, *(output.toTypedArray().apply { reverse() }))
    }

    fun toSeq(attrs: Attrs): List<Attr> = when(val a = attrs) {
      is Multiple -> a.attrs.toList()
      is Attr -> listOf(a)
      else -> throw IllegalStateException("Invalid Attrs type: ${a}")
    }
  }
}

sealed class Attr: Attrs {
  val attrs = listOf(this)
  /**
   * escapeOpt the actual ANSI escape sequence corresponding to this Attr
   */
  abstract val escapeOpt: String?

  abstract val name: String

    /**
     * Combine this [[fansi.Attr]] with one or more other [[fansi.Attr]]s
     * so they can be passed around together
     */
  override operator fun plus(other: Attrs): Attrs = Attrs(*(arrayOf(this) + Attrs.toSeq(other)))

  companion object {
    /**
     * Represents the removal of all ansi text decoration. Doesn't fit into any
     * convenient category, since it applies to them all.
     */
    val Reset = EscapeAttr(Console.RESET, Int.MAX_VALUE.toLong(), 0, Name.Unknown)

    /**
     * A list of possible categories
     */
    val categories by lazy { listOf<Category>(
        Color, // This is not lazy in Scala Fansi by since Color comes after it in this file the it would be null if the categories array was not lazy
        Back,
        Bold,
        Underlined,
        Reversed
      )
    }
  }
}



/**
 * An [[Attr]] represented by an fansi escape sequence
 */
data class EscapeAttr(val escape: String, override val resetMask: Long, override val applyMask: Long, val sourceName: Name): Attr() {
  override val escapeOpt = escape
  override val name = sourceName.value
  override fun toString() = escape + name + Console.RESET
}

/**
 * An [[Attr]] for which no fansi escape sequence exists
 */
data class ResetAttr(override val resetMask: Long, override val applyMask: Long, val sourceName: Name): Attr() {
  override val escapeOpt = null
  override val name = sourceName.value
  override fun toString() = name
}

data class SourceName(val value: String)

sealed class Category(val offset: Int, val width: Int) {
  val catName: SourceName = SourceName("???")

  val mask get(): Int =
    ((1 shl width) - 1) shl offset


  abstract val all: List<Attr>

  open fun lookupEscape(applyState: Long): String {
    val escapeOpt = lookupAttr(applyState).escapeOpt
    return if (escapeOpt != null) escapeOpt
    else ""
  }
  open fun lookupAttr(applyState: Long) = lookupAttrTable[(applyState shr offset).toInt()]

  // Allows fast lookup of categories based on the desired applyState
  open val lookupTableWidth get() = 1 shl width

  val lookupAttrTable: Array<Attr> by lazy {
    val arr = Array<Attr?>(lookupTableWidth, {null})
    for(attr in all){
      arr[(attr.applyMask shr offset).toInt()] = attr
    }
    @Suppress("UNCHECKED_CAST")
    arr as Array<Attr>
  }

  fun makeAttr(s: String, applyValue: Long, name: Name) =
    EscapeAttr(s, mask.toLong(), applyValue shl offset, name)

  fun makeNoneAttr(applyValue: Long, name: Name) = // (implicit name: sourcecode.Name)
    ResetAttr(mask.toLong(), applyValue shl offset, name) //(catName.value + "." + name.value)
}


object Bold: Category(offset = 0, width = 1){
  val On  = makeAttr(Console.BOLD, 1, Name.Unknown)
  val Off = makeNoneAttr(          0, Name.Unknown)
  override val all: List<Attr> = listOf(On, Off)
}

/**
 * [[Attr]]s to reverse the background/foreground colors of your text,
 * or un-reverse them
 */
object Reversed: Category(offset = 1, width = 1){
  val On  = makeAttr(Console.REVERSED,   1, Name.Unknown)
  val Off = makeAttr("\u001b[27m",       0, Name.Unknown)
  override val all: List<Attr> = listOf(On, Off)
}
/**
 * [[Attr]]s to enable or disable underlined text
 */
object Underlined: Category(offset = 2, width = 1){
  val On  = makeAttr(Console.UNDERLINED, 1, Name.Unknown)
  val Off = makeAttr("\u001b[24m",       0, Name.Unknown)
  override val all: List<Attr> = listOf(On, Off)
}


object Color: ColorCategory(offset = 3, width = 25, colorCode = 38){

  val Reset        = makeAttr("\u001b[39m",     0, Name.Unknown)
  val Black        = makeAttr(Console.BLACK,    1, Name.Unknown)
  val Red          = makeAttr(Console.RED,      2, Name.Unknown)
  val Green        = makeAttr(Console.GREEN,    3, Name.Unknown)
  val Yellow       = makeAttr(Console.YELLOW,   4, Name.Unknown)
  val Blue         = makeAttr(Console.BLUE,     5, Name.Unknown)
  val Magenta      = makeAttr(Console.MAGENTA,  6, Name.Unknown)
  val Cyan         = makeAttr(Console.CYAN,     7, Name.Unknown)
  val LightGray    = makeAttr("\u001b[37m",     8, Name.Unknown)
  val DarkGray     = makeAttr("\u001b[90m",     9, Name.Unknown)
  val LightRed     = makeAttr("\u001b[91m",    10, Name.Unknown)
  val LightGreen   = makeAttr("\u001b[92m",    11, Name.Unknown)
  val LightYellow  = makeAttr("\u001b[93m",    12, Name.Unknown)
  val LightBlue    = makeAttr("\u001b[94m",    13, Name.Unknown)
  val LightMagenta = makeAttr("\u001b[95m",    14, Name.Unknown)
  val LightCyan    = makeAttr("\u001b[96m",    15, Name.Unknown)
  val White        = makeAttr("\u001b[97m",    16, Name.Unknown)

  override val all: List<Attr> = listOf(
    Reset, Black, Red, Green, Yellow, Blue, Magenta, Cyan, LightGray, DarkGray,
    LightRed, LightGreen, LightYellow, LightBlue, LightMagenta, LightCyan, White
  ) + Full

}

/**
  * [[Attr]]s to set or reset the color of your background
  */
object Back: ColorCategory(offset = 28, width = 25, colorCode = 48){

  val Reset        = makeAttr("\u001b[49m",       0, Name.Unknown)
  val Black        = makeAttr(Console.BLACK_B,    1, Name.Unknown)
  val Red          = makeAttr(Console.RED_B,      2, Name.Unknown)
  val Green        = makeAttr(Console.GREEN_B,    3, Name.Unknown)
  val Yellow       = makeAttr(Console.YELLOW_B,   4, Name.Unknown)
  val Blue         = makeAttr(Console.BLUE_B,     5, Name.Unknown)
  val Magenta      = makeAttr(Console.MAGENTA_B,  6, Name.Unknown)
  val Cyan         = makeAttr(Console.CYAN_B,     7, Name.Unknown)
  val LightGray    = makeAttr("\u001b[47m",       8, Name.Unknown)
  val DarkGray     = makeAttr("\u001b[100m",      9, Name.Unknown)
  val LightRed     = makeAttr("\u001b[101m",     10, Name.Unknown)
  val LightGreen   = makeAttr("\u001b[102m",     11, Name.Unknown)
  val LightYellow  = makeAttr("\u001b[103m",     12, Name.Unknown)
  val LightBlue    = makeAttr("\u001b[104m",     13, Name.Unknown)
  val LightMagenta = makeAttr("\u001b[105m",     14, Name.Unknown)
  val LightCyan    = makeAttr("\u001b[106m",     15, Name.Unknown)
  val White        = makeAttr("\u001b[107m",     16, Name.Unknown)


  override val all: List<Attr> = listOf(
    Reset, Black, Red, Green, Yellow, Blue, Magenta, Cyan, LightGray, DarkGray,
    LightRed, LightGreen, LightYellow, LightBlue, LightMagenta, LightCyan, White
  ) + Full
}


final class Trie<T>(val strings: List<Pair<String, T>>){

  val min: Char
  val max: Char
  val arr: Array<Trie<T>?>
  val value: T?

  init {
    val (list, continuations) = strings.partition { (str, v) -> str.isEmpty() } //(_._1.isEmpty)

    when {
      list.isEmpty() -> {
        val allChildChars = continuations.map{ (a, _) -> a[0] }
        val minVal = allChildChars.min()
        val maxVal = allChildChars.max()

        val arrVal = Array<Trie<T>?>(maxVal - minVal + 1, { null })
        for( (char, ss) in continuations.groupBy { (a, _) -> a[0] } ){
          arrVal[char - minVal] = Trie(ss.map{ (k, v) -> Pair(k.drop(1), v)})
        }

        min = minVal
        max = maxVal
        arr = arrVal
        value = null
      }

      list.size == 1 && continuations.isEmpty() -> {
        val terminalValue = list.first().second
        min = 0.toChar()
        max = 0.toChar()
        arr = Array<Trie<T>?>(0, { null })
        value = terminalValue
      }

      else -> TODO()
    }
  }


  operator fun invoke(c: Char): Trie<T>? =
    if (c > max || c < min) null
    else arr[c - min]

  /**
   * Returns the length of the matching string, or -1 if not found
   */
  fun query(input: CharSequence, index: Int): Pair<Int, T>? {

    fun rec(offset: Int, currentNode: Trie<T>): Pair<Int, T>? =
      if (currentNode.value != null)
        Pair(offset - index, currentNode.value)
      else if (offset >= input.length) null
      else {
        val char = input[offset]
        val next = currentNode(char)
        if (next == null) null
        else rec(offset + 1, next)
      }

    return rec(index, this)
  }
}


/**
 * * Color a encoded on 25 bit as follow :
 * 0 : reset value
 * 1 - 16 : 3 bit colors
 * 17 - 272 : 8 bit colors
 * 273 - 16 777 388 : 24 bit colors
 */
abstract class ColorCategory(offset: Int, width: Int, val colorCode: Int): Category(offset, width /*, catName*/){ // override val catName: Name

  /**
   * 256 color <<Attr>>s, for those terminals that support it
   */
  val Full =
    (0 until 256).map { x ->
      makeAttr("\u001b[$colorCode;5;${x}m", (17 + x).toLong(), Name("Full($x)"))
    }

  private fun True0(r: Int, g: Int, b: Int, index: Int): EscapeAttr =
    makeAttr(trueRgbEscape(r, g, b), (273 + index).toLong(), Name("True(" + r + "," + g + "," + b + ")"))

  fun trueRgbEscape(r: Int, g: Int, b: Int) =
    "\u001b[" + colorCode + ";2;" + r + ";" + g + ";" + b + "m"

  /**
   * Create a TrueColor color, from a given index within the 16-million-color
   * TrueColor range
   */
  fun True(index: Int): EscapeAttr {
    require(0 <= index && index <= (1 shl 24),) {
      "True parameter `index` must be 273 <= index <= 16777488, not " + index
    }

    val r = index shr 16
    val g = (index and 0x00FF00) shr 8
    val b = index and 0x0000FF
    return True0(r, g, b, index)
  }
  /**
   * Create a TrueColor color, from a given (r, g, b) within the 16-million-color
   * TrueColor range
   */
  fun True(r: Int, g: Int, b: Int) = True0(r, g, b, trueIndex(r, g, b))

  fun trueIndex(r: Int, g: Int, b: Int): Int {
    require(0 <= r && r < 256) { "True parameter `r` must be 0 <= r < 256, not " + r }
    require(0 <= g && g < 256) { "True parameter `g` must be 0 <= r < 256, not " + g }
    require(0 <= b && b < 256) { "True parameter `b` must be 0 <= r < 256, not " + b }
    return (r shl 16) or (g shl 8) or b
  }

  override fun lookupEscape(applyState : Long): String {
    val rawIndex = (applyState shr offset).toInt()
    return if(rawIndex < 273) super.lookupEscape(applyState)
    else {
      val index = rawIndex - 273
      trueRgbEscape(r = index shr 16, g = (index and 0x00FF00) shr 8, b = index and 0x0000FF)
    }
  }
  override fun lookupAttr(applyState : Long): Attr {
    val index = (applyState shr offset).toInt()
    return if(index < 273) lookupAttrTable[index]
    else True(index - 273)
  }
  override val lookupTableWidth get() = 273
}
