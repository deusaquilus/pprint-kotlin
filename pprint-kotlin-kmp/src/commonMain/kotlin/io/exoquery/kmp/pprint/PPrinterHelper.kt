package io.exoquery.kmp.pprint

import io.exoquery.pprint.PPrinterConfig
import io.exoquery.pprint.Tree
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.*

object PPrinterHelper {
  @OptIn(ExperimentalSerializationApi::class)
  fun <T> encodeComposite(value: T, elementName: String?, descriptor: SerialDescriptor, childrenRaw: List<ChildElement>, showFieldNames: Boolean, config: PPrinterConfig): Tree {
    // important to make the sequence lazy in case doing a call to ChildElement.tree() causes an infinite loop
    val children = childrenRaw.asSequence()

    // if this isn't the root encoder, add the element to the parent
    // Note: This can proabably be more lazy with Sequences, not if if that is relevant for kotlinx-serialization.
    // Have a look at MapLikeSerializer in CollectionSerialziers.kt
    fun processChildren(showFieldNamesIfPossible: Boolean): Sequence<Tree> =
      if (showFieldNamesIfPossible)
        children.map { when (it) {
          is ChildElement.Atom -> it.tree()
          is ChildElement.Member -> Tree.KeyValue(it.name, it.tree(), it.name)
        } }
      else
        children.map { it.tree() }


    val childrenPrintsParsed =
      when (descriptor.kind) {
        is StructureKind.MAP -> {
          // In a map the serialization is a bit funny. The serializer will flatten keys and values
          // into a single list, so we need to unflatten the pairs
          val childTrees = processChildren(false)
          //require(childTrees.size % 2 == 0)
          childTrees.asSequence().chunked(2).map { (k, v) ->
            Tree.Infix(k, "->", v, null)
          }
        }
        // If it's a list the keys would be the indexes, don't bother printing that
        is StructureKind.LIST -> processChildren(false)
        // Otherwise it should be an object, only show the field names if specified
        else -> processChildren(showFieldNames)
      }

    val serialNameParsed =
      when {
        value is List<*> && config.showGenericForCollections -> "List"
        value is Map<*, *> && config.showGenericForCollections -> "Map"
        else ->
          if (config.simpleNames)
            descriptor.serialName.simplifyName()
          else
            descriptor.serialName
      }

    // After converting childrenPrintsParsed to an iterator cannot use the sequence again
    val childrenIterator = childrenPrintsParsed.iterator()
    val kind = descriptor.kind

    return when {
      // If it's an object (usually it won't have serialized fields, then just print the name of it e.g:
      // sealed interface Colors { object Red: Colors; object Blue: Colors; data class Custom(val hex: String): Colors }
      // for Colors.Red we should just print "Red"
      kind == StructureKind.OBJECT && !childrenIterator.hasNext() ->
        Tree.Literal(serialNameParsed, elementName)


      // The kotlinx-serlization mechanism will attempt to identify a polymorphic type by adding a type-field
      // and value-field on a nested structure. For a polymorphic hierarchy such as:
      // sealed interface Colors { object Red: Colors; data class Custom(val hex: String): Colors }
      // the json-serialized form of Colors.Red will be:
      // { "type": "Red", "value": {} } which in pprint will appear as Red(type="package.Red", value=Red)
      // For the class `Custom` it will be:
      // { "type": "Custom", "value": { "hex": "FF0000" } } which in pprint will appear as Custom(type="package.Custom", value=Custom(hex="FF0000"))
      // Since this kind of type discrimination is only needed for the sake of deserialization (i.e. for the deserializer to know what the actual type is)
      // it is not relevant to pretty-printing objects. Therefore, what we want to do is just to extract the value-property if possible and display just that.
      // We do that by finding the child-element with the "value" property and returning it.
      // (Note that technically this step should not be necessary since the serializer should be able to be configured to not have
      // descriminator-fields but this is currently an open feature request: https://github.com/Kotlin/kotlinx.serialization/issues/1247.
      // Also see https://github.com/Kotlin/kotlinx.serialization/issues/464.)
      kind == PolymorphicKind.OPEN || kind == PolymorphicKind.SEALED -> {
        childrenRaw.find { it is ChildElement.Member && it.name == "value" }?.let {
          it.tree()
        } ?: Tree.Apply(serialNameParsed, childrenIterator, elementName)
      }

      else -> {
        Tree.Apply(serialNameParsed, childrenIterator, elementName)
      }
    }
  }
}