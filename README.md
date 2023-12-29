# PPrint for Kotlin

This is a port of Li Haoyi's excellent Scala pretty-printing library into Kotlin.
(As well as Li Haoyi's excellent Ansi-Formatting library Fansi!)

## Usage

Add the following to your build.gradle.kts:

```kotlin
implementation("io.exoquery:pprint-kotlin:1.0.0")
```

The use the library like this: 
```kotlin
import io.exoquery.pprint

data class Name(val first: String, val last: String)
data class Person(val name: Name, val age: Int)
val p = Person(Name("Joe", "Bloggs"), 42)
println(pprint(p))
```


