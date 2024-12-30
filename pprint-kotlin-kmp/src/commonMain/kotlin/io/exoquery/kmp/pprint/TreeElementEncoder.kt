package io.exoquery.kmp.pprint

import io.exoquery.pprint.EncodeHelper
import io.exoquery.pprint.Tree
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.AbstractEncoder
import kotlinx.serialization.encoding.CompositeEncoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule

sealed interface ChildElement {
  val tree: () -> Tree

  data class Member(val name: String, val makeTree: (String) -> Tree): ChildElement {
    override val tree = { makeTree(name) }
  }
  data class Atom(override val tree: () -> Tree): ChildElement
}

data class ElemData(val fieldOnParent: String, val workingStructure: TreeElementEncoder)

object EncodeHelperImpl: EncodeHelper() {
  @OptIn(ExperimentalStdlibApi::class)
  val format = HexFormat {
    upperCase = true
    bytes.bytesPerLine
    number.removeLeadingZeros = false
  }

  @OptIn(ExperimentalStdlibApi::class)
  override fun makeHexString(c: Char): String =
    // Doing this in Kotlin Multiplatform is odd. You can't do "\\u%04x".format(c.code) because `format` is
    // only supported in Kotlin JVM. You need to use Kotlin's experimental `toHexString` functions.
    // Now if you do c.code.toByte().toHexString() then it'll truncate the character-encoding
    // to just one byte which only requires 2 digits of hex (i.e. 255 is 0xFF). If you do
    // If you do c.code.toHexString() then it assumes your character encoding is the max encoding possible i.e. UTF-32
    // and use 8 digits of hex (i.e. since the max is 4294967295 or 0xFFFFFFFF).
    // Now in the original pprint there was an assumption that unicode was done via UTF-16 (and \uXXXX escapes to it)
    // and that seems like a reasonable assumption. So in that case we want to assume that the character has two bytes
    // (whose max will be 65535 or 0xFFFF).
    // In order to get the right `toHexString` for that we do it on a 65535-max numeric value i.e. a Short.
    // (alternatively we could just do `c.code.toHexString` and then just take the last 4 digits.
    "\\u${c.code.toShort().toHexString()}"
}

fun String.simplifyName() =
  if (this.contains(".", true)) {
    this.takeLastWhile { it != '.' }
  } else {
    this
  }

data class Ancestry private constructor (val stack: List<ElemData>) {
  fun currParent() = stack.getOrNull(stack.size - 2)
  fun curr() = stack.getOrNull(stack.size - 1)
  fun push(elem: ElemData) = Ancestry(this.stack + elem)

  companion object {
    fun empty() = Ancestry(listOf())
  }
}

/**
 * This is a minimal element encoder needed to traverse nested things e.g. fields of objects
 * members of lists/maps/etc... together with their encoder data. Most of the
 * heavy lifting is done by the PPrinter to which this object delegates as much as possible.
 * Usage of this encoder in KMP is necessary for the same reason that usage of kotlinx-serialization
 * is necessary in KMP. It is because KMP does not have reflection and traversing fields of objects, lists, etc...
 * while keep track of their serializers is extremely difficult otherwise.
 */
@OptIn(ExperimentalSerializationApi::class)
@Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
open class TreeElementEncoder private constructor (val pprinter: PPrinter<*>) : Encoder, CompositeEncoder {

  companion object {
    operator fun invoke(pprinter: PPrinter<*>) = TreeElementEncoder(pprinter)
  }

  protected val escapeUnicode get() = pprinter.config.defaultEscapeUnicode
  protected val showFieldNames get() = pprinter.config.defaultShowFieldNames

  fun retrieve() = currChildren

  override val serializersModule: SerializersModule = EmptySerializersModule()

  protected val currChildren = mutableListOf<ChildElement>()

  protected fun add(name: String, tree: (name: String) -> Tree): Unit {
    currChildren.add(ChildElement.Member(name, tree))
  }

  protected fun addAtom(tree: () -> Tree): Unit {
    currChildren.add(ChildElement.Atom(tree))
  }

  private fun treeifyLeaf(value: Any?, elementName: String?): Tree =
    pprinter.treeifyElement<Any?>(PPrinter.Treeifyable.Leaf(value), elementName, escapeUnicode, showFieldNames)

  override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder = this

  override fun endStructure(descriptor: SerialDescriptor): Unit {}


  /**
   * Invoked before writing an element that is part of the structure to determine whether it should be encoded.
   * Element information can be obtained from the [descriptor] by the given [index].
   *
   * @return `true` if the value should be encoded, false otherwise
   */
  public fun encodeElement(descriptor: SerialDescriptor, index: Int): Boolean = true

  /**
   * Invoked to encode a value when specialized `encode*` method was not overridden.
   */
  public fun encodeValue(value: Any): Unit = addAtom { treeifyLeaf(value, null) }

  override fun encodeNull(): Unit = addAtom { treeifyLeaf(null, null) }

  override fun encodeInline(descriptor: SerialDescriptor): Encoder = this

  // Delegating implementation of CompositeEncoder
  override fun encodeBooleanElement(desc: SerialDescriptor, index: Int, value: Boolean) = add(desc.getElementName(index), { treeifyLeaf(value, it) })
  override fun encodeByteElement(desc: SerialDescriptor, index: Int, value: Byte) = add(desc.getElementName(index), { treeifyLeaf(value, it) })
  override fun encodeShortElement(desc: SerialDescriptor, index: Int, value: Short) = add(desc.getElementName(index), { treeifyLeaf(value, it) })
  override fun encodeIntElement(desc: SerialDescriptor, index: Int, value: Int) = add(desc.getElementName(index), { treeifyLeaf(value, it) })
  override fun encodeLongElement(desc: SerialDescriptor, index: Int, value: Long) = add(desc.getElementName(index), { treeifyLeaf(value, it) })
  override fun encodeFloatElement(desc: SerialDescriptor, index: Int, value: Float) = add(desc.getElementName(index), { treeifyLeaf(value, it) })
  override fun encodeDoubleElement(desc: SerialDescriptor, index: Int, value: Double) = add(desc.getElementName(index), { treeifyLeaf(value, it) })
  override fun encodeCharElement(desc: SerialDescriptor, index: Int, value: Char) = add(desc.getElementName(index), { treeifyLeaf(value, it) })
  override fun encodeStringElement(desc: SerialDescriptor, index: Int, value: String) = add(desc.getElementName(index), { treeifyLeaf(value, it) })

  override fun encodeBoolean(value: Boolean): Unit = addAtom { treeifyLeaf(value, null) }
  override fun encodeByte(value: Byte): Unit = addAtom { treeifyLeaf(value, null) }
  override fun encodeShort(value: Short): Unit = addAtom { treeifyLeaf(value, null) }
  override fun encodeInt(value: Int): Unit = addAtom { treeifyLeaf(value, null) }
  override fun encodeLong(value: Long): Unit = addAtom { treeifyLeaf(value, null) }
  override fun encodeFloat(value: Float): Unit = addAtom { treeifyLeaf(value, null) }
  override fun encodeDouble(value: Double): Unit = addAtom { treeifyLeaf(value, null) }
  override fun encodeChar(value: Char): Unit = addAtom { treeifyLeaf(value, null) }
  override fun encodeString(value: String): Unit = addAtom { treeifyLeaf(value, null) }

  override fun encodeEnum(enumDescriptor: SerialDescriptor, index: Int): Unit = addAtom { treeifyLeaf(enumDescriptor.getElementName(index), enumDescriptor.getElementName(index)) }

  override fun beginCollection(descriptor: SerialDescriptor, collectionSize: Int): CompositeEncoder {
    return super.beginCollection(descriptor, collectionSize)
  }

  final override fun encodeInlineElement(
    descriptor: SerialDescriptor,
    index: Int
  ): Encoder =
    if (encodeElement(descriptor, index)) encodeInline(descriptor.getElementDescriptor(index)) else NoOpEncoder

  override fun <T : Any?> encodeSerializableElement(
    descriptor: SerialDescriptor,
    index: Int,
    serializer: SerializationStrategy<T>,
    value: T
  ) {
    if (encodeElement(descriptor, index)) {
      val elemName = descriptor.getElementName(index)
      add(elemName) {
        pprinter.treeifyElement<T>(PPrinter.Treeifyable.Elem(value, serializer), it, escapeUnicode, showFieldNames)
      }
    }
  }

  override fun <T : Any> encodeNullableSerializableElement(
    descriptor: SerialDescriptor,
    index: Int,
    serializer: SerializationStrategy<T>,
    value: T?
  ) {
    if (encodeElement(descriptor, index)) {
      if (value == null) encodeNull()
      else encodeSerializableElement(descriptor, index, serializer, value)
    }
  }
}

@ExperimentalSerializationApi
internal object NoOpEncoder : AbstractEncoder() {
  override val serializersModule: SerializersModule = EmptySerializersModule()

  public override fun encodeValue(value: Any): Unit = Unit
  override fun encodeNull(): Unit = Unit

  override fun encodeBoolean(value: Boolean): Unit = Unit
  override fun encodeByte(value: Byte): Unit = Unit
  override fun encodeShort(value: Short): Unit = Unit
  override fun encodeInt(value: Int): Unit = Unit
  override fun encodeLong(value: Long): Unit = Unit
  override fun encodeFloat(value: Float): Unit = Unit
  override fun encodeDouble(value: Double): Unit = Unit
  override fun encodeChar(value: Char): Unit = Unit
  override fun encodeString(value: String): Unit = Unit
  override fun encodeEnum(enumDescriptor: SerialDescriptor, index: Int): Unit = Unit
}


