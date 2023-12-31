package io.exoquery.pprint

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.AbstractEncoder
import kotlinx.serialization.encoding.CompositeEncoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule

sealed interface ChildElement {
  val tree: Tree

  data class Member(val name: String, override val tree: Tree): ChildElement
  data class Atom(override val tree: Tree): ChildElement
}

data class ParentData(val currFieldOnParent: String, val parentEncoder: TreeElementEncoder)


@OptIn(ExperimentalSerializationApi::class)
@Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
class TreeElementEncoder private constructor (val showFieldNames: Boolean, val escapeUnicode: Boolean, val parent: ParentData?) : Encoder, CompositeEncoder {
  companion object {
    operator fun invoke(showFieldNames: Boolean, escapeUnicode: Boolean) =
      TreeElementEncoder(showFieldNames, escapeUnicode, null)
  }
  fun retrieve(): Tree =
    when {
      // Root element should only have one child and that should be the top-level thing encoded
      currChildren.isEmpty() ->
        throw IllegalStateException("Top level encoder-context is empty")
      currChildren.size > 1 ->
        throw IllegalStateException(
          """|Multiple elements in top level encoder:
             |${currChildren.withIndex().map { (idx, v) -> "${idx}) $v" }.joinToString("\n")}
             |""".trimMargin()
        )
      else -> currChildren.first().tree
    }

  private var currIndex = 0

  override val serializersModule: SerializersModule = EmptySerializersModule()

  private val currChildren = mutableListOf<ChildElement>()

  private fun add(name: String, tree: Tree): Unit {
    currChildren.add(ChildElement.Member(name, tree))
  }

  private fun addAtom(tree: Tree): Unit {
    currChildren.add(ChildElement.Atom(tree))
  }

  override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder =
    TreeElementEncoder(showFieldNames, escapeUnicode, ParentData(descriptor.getElementName(currIndex), this))

  override fun endStructure(descriptor: SerialDescriptor) {
    // if this isn't the root encoder, add the element to the parent
    // Note: This can proabably be more lazy with Sequences, not if if that is relevant for kotlinx-serialization
    val childrenTrees =
      if (showFieldNames)
        currChildren.map { when (it) {
          is ChildElement.Atom -> it.tree
          is ChildElement.Member -> Tree.KeyValue(it.name, it.tree)
        } }
      else
        currChildren.map { it.tree }

    val currElement = Tree.Apply(descriptor.serialName, childrenTrees.iterator())

    if (parent != null) parent.parentEncoder.add(descriptor.getElementName(currIndex), currElement)
    else throw IllegalStateException(
      """|Attempted to write value for ${descriptor.serialName} into an empty parent context
         |============== Descriptor is: ==============
         |${descriptor}
         |============== Value is: ==============
         |${currElement}
      """.trimMargin())
  }


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
    public fun encodeValue(value: Any): Unit =
        throw SerializationException("Non-serializable ${value::class} is not supported by ${this::class} encoder")

    override fun encodeNull(): Unit = TODO() // add(Tree.Literal("null"))

    override fun encodeInline(descriptor: SerialDescriptor): Encoder = this

    // Delegating implementation of CompositeEncoder
    override fun encodeBooleanElement(desc: SerialDescriptor, index: Int, value: Boolean) = add(desc.getElementName(index), Tree.Literal(value.toString()))
    override fun encodeByteElement(desc: SerialDescriptor, index: Int, value: Byte) = add(desc.getElementName(index), Tree.Literal(value.toString()))
    override fun encodeShortElement(desc: SerialDescriptor, index: Int, value: Short) = add(desc.getElementName(index), Tree.Literal(value.toString()))
    override fun encodeIntElement(desc: SerialDescriptor, index: Int, value: Int) = add(desc.getElementName(index), Tree.Literal(value.toString()))
    override fun encodeLongElement(desc: SerialDescriptor, index: Int, value: Long) = add(desc.getElementName(index), Tree.Literal(value.toString()))
    override fun encodeFloatElement(desc: SerialDescriptor, index: Int, value: Float) = add(desc.getElementName(index), Tree.Literal(value.toString()))
    override fun encodeDoubleElement(desc: SerialDescriptor, index: Int, value: Double) = add(desc.getElementName(index), Tree.Literal(value.toString()))
    override fun encodeCharElement(desc: SerialDescriptor, index: Int, value: Char) = add(desc.getElementName(index), Tree.encodeChar(value, escapeUnicode))
    override fun encodeStringElement(desc: SerialDescriptor, index: Int, value: String) = add(desc.getElementName(index), Tree.encodeString(value, escapeUnicode))

    override fun encodeBoolean(value: Boolean): Unit = addAtom(Tree.Literal(value.toString()))
    override fun encodeByte(value: Byte): Unit = addAtom(Tree.Literal(value.toString()))
    override fun encodeShort(value: Short): Unit = addAtom(Tree.Literal(value.toString()))
    override fun encodeInt(value: Int): Unit = addAtom(Tree.Literal(value.toString()))
    override fun encodeLong(value: Long): Unit = addAtom(Tree.Literal(value.toString()))
    override fun encodeFloat(value: Float): Unit = addAtom(Tree.Literal(value.toString()))
    override fun encodeDouble(value: Double): Unit = addAtom(Tree.Literal(value.toString()))
    override fun encodeChar(value: Char): Unit = addAtom(Tree.encodeChar(value, escapeUnicode))
    override fun encodeString(value: String): Unit = addAtom(Tree.encodeString(value, escapeUnicode))
    override fun encodeEnum(enumDescriptor: SerialDescriptor, index: Int): Unit = TODO("Enums not supported yet")

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
          currIndex = index
          encodeSerializableValue(serializer, value)
        }
    }

    override fun <T : Any> encodeNullableSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        serializer: SerializationStrategy<T>,
        value: T?
    ) {
        if (encodeElement(descriptor, index))
            encodeNullableSerializableValue(serializer, value)
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
