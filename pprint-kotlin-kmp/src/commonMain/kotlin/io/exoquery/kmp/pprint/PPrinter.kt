package io.exoquery.kmp.pprint

import io.exoquery.fansi.Attrs
import io.exoquery.pprint.PPrinterBase
import io.exoquery.pprint.PPrinterConfig
import io.exoquery.pprint.Tree
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.serializer

open class PPrinter<T>(open val serializer: SerializationStrategy<T>, override open val config: PPrinterConfig = PPrinterConfig()): PPrinterBase<T>(config) {

  companion object {
    inline operator fun <reified T> invoke(config: PPrinterConfig = PPrinterConfig()) = PPrinter(serializer<T>(), config)
    inline fun <reified T> Color(serializer: SerializationStrategy<T> = serializer<T>(), config: PPrinterConfig = PPrinterConfig()) = PPrinter(serializer, config)
    inline fun <reified T> BlackWhite(serializer: SerializationStrategy<T> = serializer<T>(), config: PPrinterConfig = PPrinterConfig()) =
      PPrinter(serializer, config.copy(
          colorLiteral = Attrs.Empty,
          colorApplyPrefix = Attrs.Empty
        )
      )

    val defaultModule = SerializersModule {
      contextual(Iterator::class) { params -> PPrintIteratorSerializer(params.first()) }
      contextual(Sequence::class) { params -> PPrintSequenceSerializer(params.first()) }
    }
  }

  sealed interface Treeifyable<E> {
    val value: E

    data class Leaf(override val value: Any?): Treeifyable<Any?>
    data class Elem<E>(override val value: E, val serializer: SerializationStrategy<E>): Treeifyable<E>
  }


  override fun treeify(x: T, escapeUnicode: Boolean, showFieldNames: Boolean): Tree =
    treeifyWith(Treeifyable.Elem(x, serializer), escapeUnicode, showFieldNames)

  open fun <R> treeifyWith(treeifyable: Treeifyable<R>, escapeUnicode: Boolean, showFieldNames: Boolean): Tree {
    val value = treeifyable.value
    return when {
      value == null -> Tree.Literal("null")
      value is Boolean -> Tree.Literal(value.toString())
      value is Byte -> Tree.Literal(value.toString())
      value is Short -> Tree.Literal(value.toString())
      value is Int -> Tree.Literal(value.toString())
      value is Long -> Tree.Literal("${value}L")
      value is Float -> Tree.Literal("${value}F")
      value is Double -> Tree.Literal(value.toString())
      value is Char -> EncodeHelperImpl.encodeChar(value, escapeUnicode)
      value is String -> EncodeHelperImpl.encodeString(value, escapeUnicode)
      // Empty iterator case which for some reason causes an exception with the regular kotlin serializer
      value is Iterator<*> && !value.hasNext() -> Tree.Literal("empty iterator")
      value is Iterator<*> && value.hasNext() -> Tree.Literal("non-empty iterator")
      // If it is a sequence and the user is using the correct serializer i.e. PPrintSequenceSerializer
      value is Sequence<*> && treeifyable is Treeifyable.Elem && treeifyable.serializer is PPrintSequenceSerializer<*> -> {
        @Suppress("UNCHECKED_CAST")
        val elementSerializer = treeifyable.serializer.element as KSerializer<Any?>
        Tree.Apply("Sequence", value.map { treeifyWith(Treeifyable.Elem(it, elementSerializer), escapeUnicode, showFieldNames) }.iterator())
      }
      treeifyable is Treeifyable.Elem -> {
        val encoder = TreeElementEncoder(this)
        treeifyable.serializer.serialize(encoder, treeifyable.value)
        val elements = encoder.retrieve()
        PPrinterHelper.encodeComposite(
          treeifyable.value,
          treeifyable.serializer.descriptor,
          elements,
          showFieldNames,
          config
        )
      }
      else -> Tree.Literal(value.toString())
    }
  }
}

class PPrintSequenceSerializer<T>(val element: KSerializer<T>) : KSerializer<Sequence<T>> {
  override val descriptor: SerialDescriptor = element.descriptor
  override fun serialize(encoder: Encoder, value: Sequence<T>) = throw IllegalStateException("PPrint sequence serializer only used as a proxy for the element-serializer. Should not be used directly.")
  override fun deserialize(decoder: Decoder) = throw IllegalStateException("PPrint sequence serializer does not support deserialization.")
}

class PPrintIteratorSerializer<T>(val element: KSerializer<T>) : KSerializer<Iterator<T>> {
  override val descriptor: SerialDescriptor = element.descriptor
  override fun serialize(encoder: Encoder, value: Iterator<T>) = throw IllegalStateException("PPrint sequence serializer only used as a proxy for the element-serializer. Should not be used directly.")
  override fun deserialize(decoder: Decoder) = throw IllegalStateException("PPrint sequence serializer does not support deserialization.")
}
