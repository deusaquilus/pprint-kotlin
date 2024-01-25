# PPrint for Kotlin

This is a port of Li Haoyi's excellent Scala pretty-printing library into Kotlin [PPrint](https://github.com/com-lihaoyi/PPrint).
(As well as Li Haoyi's excellent Ansi-Formatting library Fansi!)

## Usage

Add the following to your build.gradle.kts:

```kotlin
implementation("io.exoquery:pprint-kotlin:1.0.0")
```

Then use the library like this: 
```kotlin
import io.exoquery.pprint

data class Name(val first: String, val last: String)
data class Person(val name: Name, val age: Int)
val p = Person(Name("Joe", "Bloggs"), 42)
println(pprint(p))
```

It will print the following beautiful output:

## <img src="https://github.com/deusaquilus/pprint-kotlin/assets/1369480/ce866664-7959-46fb-a8c8-9a636a315281" width=50% height=50%>

PPrint-Kotlin supports most of the same features and options as the Scala version.
I will document them here over time however for now please refer to the Scala documentation
* For PPrint here - https://github.com/com-lihaoyi/PPrint
* For Fansi here - https://github.com/com-lihaoyi/fansi

## Nested Data and Complex Collections

PPrint excels at printing nested data structures and complex collections. For example:

#### Lists embedded in objects:
```kotlin
data class Address(val street: String, val zip: Int)
data class Customer(val name: Name, val addresses: List<Address>)

val p = Customer(Name("Joe", "Bloggs"), listOf(Address("foo", 123), Address("bar", 456), Address("baz", 789)))
println(pprint(p))
```


## <img src="https://github.com/deusaquilus/pprint-kotlin/assets/1369480/3c7b7e18-d246-451c-ae3d-bfcc102ccefc" width=50% height=50%>


#### Maps embedded in objects:
```kotlin
data class Alias(val value: String)
data class ComplexCustomer(val name: Name, val addressAliases: Map<Alias, Address>)

val p =
  ComplexCustomer(
    Name("Joe", "Bloggs"),
    mapOf(Alias("Primary") to Address("foo", 123), Alias("Secondary") to Address("bar", 456), Alias("Tertiary") to Address("baz", 789))
  )
println(pprint(p))
```

## <img src="https://github.com/deusaquilus/pprint-kotlin/assets/1369480/813afad2-1cfa-4629-b2a8-253ac47254a4" width=50% height=50%>


#### Lists embedded in maps embedded in objects:

```kotlin
val p =
  VeryComplexCustomer(
    Name("Joe", "Bloggs"),
    mapOf(
      Alias("Primary") to
        listOf(Address("foo", 123), Address("foo1", 123), Address("foo2", 123)),
      Alias("Secondary") to
        listOf(Address("bar", 456), Address("bar1", 456), Address("bar2", 456)),
      Alias("Tertiary") to
        listOf(Address("baz", 789), Address("baz1", 789), Address("baz2", 789))
    )
  )
println(pprint(p))
```

## <img src="https://github.com/deusaquilus/pprint-kotlin/assets/1369480/4f3aeb69-315f-4fd7-b831-c568c6daa26c" width=50% height=50%>

## Removing Field Names

By default pprint will print the field names of data classes. You can remove these by using `showFieldNames = false`:

```kotlin
val p = Person(Name("Joe", "Bloggs"), 42)
println(pprint(p, showFieldNames = false))
```

For larger ADTs this dramatically reduces the amount of output and often improves the readability.

## User-controlled Width

Another nice feature of PPrint is that it can print data classes with a user-controlled width.

```kotlin
println(pprint(p, showFieldNames = false, defaultWidth = 30)) // Narrow
```
<img src="https://github.com/deusaquilus/pprint-kotlin/assets/1369480/186047f4-dcfe-4331-9bd3-23f51549548a" width=50% height=50%>

```kotlin
println(pprint(p, showFieldNames = false, defaultWidth = 100)) // Wide
```

## <img src="https://github.com/deusaquilus/pprint-kotlin/assets/1369480/c6539ed6-0584-4233-87d6-224b35e011b6" width=70% height=70%>

## Infinite Sequences

Another very impressive ability of PPrint is that it can print infinite sequences, even if they are embedded
other objects for example:
```kotlin
data class SequenceHolder(val seq: Sequence<String>)

var i = 0
val p = SequenceHolder(generateSequence { "foo-${i++}" })
println(pprint(p, defaultHeight = 10))
```

## <img src="https://github.com/deusaquilus/pprint-kotlin/assets/1369480/9026f8ca-479e-442d-966b-0c1f1f887986" width=50% height=50%>



PPrint is able to print this infinite sequence without stack-overflowing or running out of memory
because it is highly lazy. It only evaluates the sequence as it is printing it,
and the printing is always constrained by the height and width of the output. You can
control these with the `defaultHeight` and `defaultWidth` parameters to the `pprint` function.

## Circular References

Similar to infinite sequences, PPrint will print circular references up to the specified defaultHeight after which the output will be truncated.
```kotlin
data class Parent(var child: Child?)
data class Child(var parent: Parent?)

val child = Child(parent = null)
val parent = Parent(child = null)
child.parent = parent
parent.child = child
println(pprint(parent, defaultHeight = 10))
```

## <img src="https://github.com/deusaquilus/pprint-kotlin/assets/1369480/146c78eb-11e8-4cdb-a547-76ac9d79ce91" width=50% height=50%>


## Black & White Printing

The output of the pprint function is not actually a java.lang.String, but a fansi.Str. This
means you can control how it is printed. For example, to print it in black and white simple do:
```kotlin
import io.exoquery.pprint.PPrinter

val p = Person(Name("Joe", "Bloggs"), 42)

// Use Black & White Printing
println(pprint(p).plainText)
```

## Extending PPrint

In order to extend pprint, subclass the PPrinter class and override the `treeify` function.
For example:
```kotlin
class CustomPPrinter1(config: PPrinterConfig) : PPrinter(config) {
  override fun treeify(x: Any?, escapeUnicode: Boolean, showFieldNames: Boolean): Tree =
    when (x) {
      is java.time.LocalDate -> Tree.Literal(x.format(DateTimeFormatter.ofPattern("MM/dd/YYYY")))
      else -> super.treeify(x, escapeUnicode, showFieldNames)
    }
}

data class Person(val name: String, val born: LocalDate)
val pp = CustomPPrinter1(PPrinterConfig())
val joe = Person("Joe", LocalDate.of(1981, 1, 1))

println(pp.invoke(joe))
//> Person(name = "Joe", born = 01/01/1981)
```

For nested objects use Tree.Apply and recursively call the treeify method.
```kotlin
// A class that wouldn't normally print the right thing with pprint...
class MyJavaBean(val a: String, val b: Int) {
  fun getValueA() = a
  fun getValueB() = b
}

// Create the custom printer
class CustomPPrinter2(config: PPrinterConfig) : PPrinter(config) {
  override fun treeify(x: Any?, esc: Boolean, names: Boolean): Tree =
    when (x) {
      // List through the properties of 'MyJavaBean' and recursively call treeify on them.
      // (Note that Tree.Apply takes an iterator of properties so that the interface is lazy)
      is MyJavaBean -> Tree.Apply("MyJavaBean", listOf(x.getValueA(), x.getValueB()).map { treeify(it, esc, names) }.iterator())
      else -> super.treeify(x, esc, names)
    }
}

val bean = MyJavaBean("abc", 123)
val pp = CustomPPrinter2(PPrinterConfig())
println(pp.invoke(bean))
//> MyJavaBean("abc", 123)
```

To print field-names you use Tree.KeyValue:
```kotlin
class CustomPPrinter3(config: PPrinterConfig) : PPrinter(config) {
  override fun treeify(x: Any?, escapeUnicode: Boolean, showFieldNames: Boolean): Tree {
    // function to make recursive calls shorter
    fun rec(x: Any?) = treeify(x, escapeUnicode, showFieldNames)
    return when (x) {
      // Recurse on the values, pass result into Tree.KeyValue.
      is MyJavaBean -> 
        Tree.Apply(
          "MyJavaBean", 
          listOf(Tree.KeyValue("a", rec(x.getValueA())), Tree.KeyValue("b", rec(x.getValueB()))).iterator()
        )
      else -> 
        super.treeify(x, esc, names)
    }
  }
}

val bean = MyJavaBean("abc", 123)
val pp = CustomPPrinter2(PPrinterConfig())
println(pp.invoke(bean))
//> MyJavaBean(a = "abc", b = 123)
```

Often it is a good idea to honor the `showFieldNames` parameter only display key-values if it is enabled:
```kotlin
class CustomPPrinter4(config: PPrinterConfig) : PPrinter(config) {
  override fun treeify(x: Any?, escapeUnicode: Boolean, showFieldNames: Boolean): Tree {
    // function to make recursive calls shorter
    fun rec(x: Any?) = treeify(x, escapeUnicode, showFieldNames)
    fun field(fieldName: String, value: Any?) =
      if (showFieldNames) Tree.KeyValue(fieldName, rec(value)) else rec(value) 
    return when (x) {
      // Recurse on the values, pass result into Tree.KeyValue.
      is MyJavaBean -> 
        Tree.Apply("MyJavaBean", listOf(field("a", x.getValueA()), field("b", x.getValueB())).iterator())
      else -> 
        super.treeify(x, escapeUnicode, showFieldNames)
    }
  }
}

val bean = MyJavaBean("abc", 123)
println(CustomPPrinter4(PPrinterConfig()).invoke(bean))
//> MyJavaBean(a = "abc", b = 123)
println(CustomPPrinter4(PPrinterConfig(defaultShowFieldNames = false)).invoke(bean))
//> MyJavaBean("abc", 123)
```


# Fansi for Kotlin

> NOTE. Most of this is taken from the original Fansi documentation [here](https://raw.githubusercontent.com/com-lihaoyi/fansi/master/readme.md)

Fansi is a Kotlin library (ported from Scala) that was designed make it easy to deal with fancy colored Ansi
strings within your command-line programs.

While "normal" use of Ansi escapes with `java.lang.String`, you find yourself
concatenating colors:

```scala
val colored = Console.RED + "Hello World Ansi!" + Console.RESET
```

To build your colored string. This works the first time, but is error prone
on larger strings: e.g. did you remember to put a `Console.RESET` where it's
necessary? Do you need to end with one to avoid leaking the color to the entire
console after printing it?

Furthermore, some operations are fundamentally difficult or error-prone with
this approach. For example,

```scala
val colored: String = Console.RED + "Hello World Ansi!" + Console.RESET

// How to efficiently get the length of this string on-screen? We could try
// using regexes to remove and Ansi codes, but that's slow and inefficient.
// And it's easy to accidentally call `colored.length` and get a invalid length
val length = ???

// How to make the word `World` blue, while preserving the coloring of the
// `Ansi!` text after? What if the string came from somewhere else and you
// don't know what color that text was originally?
val blueWorld = ???

// What if I want to underline "World" instead of changing it's color, while
// still preserving the original color?
val underlinedWorld = ???

// What if I want to apply underlines to "World" and the two characters on
// either side, after I had already turned "World" blue?
val underlinedBlue = ???
```

While simple to describe, these tasks are all error-prone and difficult to
do using normal `java.lang.String`s containing Ansi color codes. This is
especially so if, unlike the toy example above, `colored` is coming from some
other part of your program and you're not sure what or how-many Ansi color
codes it already contains.

With Fansi, doing all these tasks is simple, error-proof and efficient:

```scala
val colored: fansi.Str = fansi.Color.Red("Hello World Ansi!")
// Or fansi.Str("Hello World Ansi!").overlay(fansi.Color.Red)

val length = colored.length // Fast and returns the non-colored length of string

val blueWorld = colored.overlay(fansi.Color.Blue, 6, 11)

val underlinedWorld = colored.overlay(fansi.Underlined.On, 6, 11)

val underlinedBlue = blueWorld.overlay(fansi.Underlined.On, 4, 13)
```

And it just works:

![image](https://github.com/deusaquilus/pprint-kotlin/assets/1369480/d9b14cff-0527-41b7-96a7-25aa616f76aa)

Why Fansi?
----------

Unlike normal `java.lang.String`s with Ansi escapes embedded inside,
`fansi.Str` allows you to perform a range of operations in an efficient
manner:

- Extracting the non-Ansi `plainText` version of the string

- Get the non-Ansi `length`

- Concatenate colored Ansi strings without worrying about leaking
  colors between them

- Applying colors to certain portions of an existing `fansi.Str`,
  and ensuring that the newly-applied colors get properly terminated
  while existing colors are unchanged

- Splitting colored Ansi strings at a `plainText` index

- Rendering to colored `java.lang.String`s with Ansi escapes embedded,
  which can be passed around or concatenated without worrying about
  leaking colors.

These are tasks which are possible to do with normal `java.lang.String`,
but are tedious, error-prone and typically inefficient. Often, you can get
by with adding copious amounts of `Console.RESET`s when working with colored
`java.lang.String`s, but even that easily results in errors when you `RESET`
too much and stomp over colors that already exist:

![image](https://github.com/deusaquilus/pprint-kotlin/assets/1369480/792d08b6-4594-477f-acfb-e095c921e5e9)


`fansi.Str` allows you to perform these tasks safely and easily:

![image](https://github.com/deusaquilus/pprint-kotlin/assets/1369480/41a916dd-0605-4879-8ad2-b49c2516461c)

Fansi is also very efficient: `fansi.Str` uses just 3x as much memory as
`java.lang.String` to hold all the additional formatting information.

> Note this was the case in Scala, I am not certain if the same is true in Kotlin.

Its operations are probably about the same factor slower, as they are all
implemented using fast `arraycopy`s and while-loops similar to
`java.lang.String`. That means that - unlike fiddling with Ansi-codes using
regexes - you generally do not need to worry about performance when dealing with
`fansi.Str`s. Just treat them as you would `java.lang.String`s: splitting them,
`substring`ing them, and applying or removing colors or other styles at-will.

Using Fansi
-----------

The main operations you need to know are:

- Str(raw: CharSequence): fansi.String`, to construct colored
  Ansi strings from a `java.lang.String`, with or without existing Ansi
  color codes inside it.

- `Str`, the primary data-type that you will use to pass-around
  colored Ansi strings and manipulate them: concatenating, splitting,
  applying or removing colors, etc.

![image](https://github.com/deusaquilus/pprint-kotlin/assets/1369480/46422458-8406-459c-bd44-578f1465a9ea)

- `fansi.Attr`s are the individual modifications you can make to an
  `fansi.Str`'s formatting. Examples are:
    - `fansi.Bold.{On, Off}`
    - `fansi.Reversed.{On, Off}`
    - `fansi.Underlined.{On, Off}`
    - `fansi.Color.*`
    - `fansi.Back.*`
    - `fansi.Attr.Reset`

![image](https://github.com/deusaquilus/pprint-kotlin/assets/1369480/a505dc23-186c-450f-8dd6-2af5616c0420)

- `fansi.Attrs` represents a group of zero or more `fansi.Attr`s.
  These that can be passed around together, combined via `++` or applied
  to `fansi.Str`s all at once. Any individual `fansi.Attr` can be used
  when `fansi.Attrs` is required, as can `fansi.Attrs.empty`.

![image](https://github.com/deusaquilus/pprint-kotlin/assets/1369480/b9c67518-0e85-4a27-9dca-14449920f983)

- Using any of the `fansi.Attr` or `fansi.Attrs` mentioned above, e.g.
  `fansi.Color.Red`, using `fansi.Color.Red("hello world ansi!")` to create a
  `fansi.Str` with that text and color, or
  `fansi.Str("hello world ansi!").overlay(fansi.Color.Blue, 6, 11)`

- `.render` to convert a `fansi.Str` back into a `java.lang.String` with all
  necessary Ansi color codes within it

Fansi also supports 8-bit 256-colors through `fansi.Color.Full` and
`fansi.Back.Full`, as well as 24-bit 16-million-colors through
`fansi.Color.True` and `fansi.Back.True`:

![image](https://github.com/deusaquilus/pprint-kotlin/assets/1369480/f1c5c6b8-597c-448f-9df2-d19698d2ca16)

Note that Fansi only performs the rendering of the colors to an ANSI-encoded
string. Final rendering will depend on whichever terminal you print the string
to, whether it is able to display these sets of colors or not.

_Thanks so much to Li Haoyi for building Fansi and PPrint!!_

