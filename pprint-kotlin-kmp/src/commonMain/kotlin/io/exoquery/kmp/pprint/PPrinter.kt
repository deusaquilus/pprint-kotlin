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

abstract class PPrinterKmp<T>(override open val config: PPrinterConfig): PPrinterBase<T>(config) {
  open fun <R> treeifyValueOrNull(value: R, elementName: String?, escapeUnicode: Boolean, showFieldNames: Boolean): Tree? =
    when {
      value == null -> Tree.Literal("null", elementName)
      value is Boolean -> Tree.Literal(value.toString(), elementName)
      value is Byte -> Tree.Literal(value.toString(), elementName)
      value is Short -> Tree.Literal(value.toString(), elementName)
      value is Int -> Tree.Literal(value.toString(), elementName)
      value is Long -> Tree.Literal("${value}L", elementName)
      value is Float -> Tree.Literal("${value}F", elementName)
      value is Double -> Tree.Literal(value.toString(), elementName)
      value is Char -> EncodeHelperImpl.encodeChar(value, escapeUnicode, elementName)
      value is String -> EncodeHelperImpl.encodeString(value, escapeUnicode, elementName)
      // Empty iterator case which for some reason causes an exception with the regular kotlin serializer
      value is Iterator<*> && !value.hasNext() -> Tree.Literal("empty iterator", elementName)
      value is Iterator<*> && value.hasNext() -> Tree.Literal("non-empty iterator", elementName)
      else -> null
    }
}

open class PPrinterManual<T>(override val config: PPrinterConfig = PPrinterConfig()): PPrinterKmp<T>(config) {
  override fun treeify(x: T, elementName: String?, escapeUnicode: Boolean, showFieldNames: Boolean): Tree =
    treeifyValueOrNull(x, elementName, escapeUnicode, showFieldNames)
      ?: Tree.Literal(x.toString(), elementName)
}

open class PPrinter<T>(open val serializer: SerializationStrategy<T>, override open val config: PPrinterConfig = PPrinterConfig()): PPrinterKmp<T>(config) {

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


  override fun treeify(x: T, elementName: String?, escapeUnicode: Boolean, showFieldNames: Boolean): Tree =
    treeifyElement(Treeifyable.Elem(x, serializer), elementName, escapeUnicode, showFieldNames)

  open fun <E> treeifyComposite(elem: Treeifyable.Elem<E>, elementName: String?, showFieldNames: Boolean) = run {
    val encoder = TreeElementEncoder(this)
    elem.serializer.serialize(encoder, elem.value)
    val elements = encoder.retrieve()
    PPrinterHelper.encodeComposite(
      elem.value,
      elementName,
      elem.serializer.descriptor,
      elements,
      showFieldNames,
      config
    )
  }

  open fun <R> treeifyElement(treeifyable: Treeifyable<R>, elementName: String?, escapeUnicode: Boolean, showFieldNames: Boolean): Tree {
    return when(treeifyable) {
      is Treeifyable.Elem<*> ->
        // try to treeify it with the treeifyValueOrNull function in case there's an override for the element
        treeifyValueOrNull(treeifyable.value, elementName, escapeUnicode, showFieldNames) ?: run {
          when {
            // If it is a sequence and the user is using the correct serializer i.e. PPrintSequenceSerializer
            treeifyable.value is Sequence<*> && treeifyable.serializer is PPrintSequenceSerializer<*> -> {
              @Suppress("UNCHECKED_CAST")
              val elementSerializer = treeifyable.serializer.element as KSerializer<Any?>
              Tree.Apply("Sequence", (treeifyable.value as Sequence<*>).map { treeifyElement(Treeifyable.Elem(it, elementSerializer), null, escapeUnicode, showFieldNames) }.iterator(), elementName)
            }
            // Otherwise it's a regular composite
            else -> treeifyComposite(treeifyable, elementName, showFieldNames)
          }
        }
      is Treeifyable.Leaf ->
        treeifyValueOrNull(treeifyable.value, elementName, escapeUnicode, showFieldNames) ?:
          Tree.Literal(treeifyable.value.toString(), elementName)
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
