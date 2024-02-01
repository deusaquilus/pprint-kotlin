package io.exoquery.pprint

import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.memberProperties

// Thing passed in must exist but the result could be null which is why its <Any, Any?> in KProperty1
// the first Any is the type of the class, the second is the type of the property
// also note that KProperty1 implements (T) -> R which is this case is (Any) -> Any?
// this is neat because KProperty is a lambda representing a getter
@Suppress("UNCHECKED_CAST")
fun KClass<*>.dataClassProperties(): List<KProperty1<Any, Any?>> {
  val constructorParams = this.constructors.first().parameters.map { it.name }.toSet()
  // NOTE: What is the typical number of member-props. Should this be put into a hashmap? Should there be caching?
  val members = this.memberProperties
  fun failNotFound(paramName: String?): Nothing =
    throw IllegalStateException("The parameter name '${paramName}' of ${this.qualifiedName ?: "<Unknown Data Class>"} could not be found within the list of members: ${members}")

  val props = constructorParams.map { param -> members.find { it.name == param } ?: failNotFound(param) }

  return props as List<KProperty1<Any, Any?>>
}

object ProductSupport {

  fun treeifyProductElements(
    x: Any,
    cls: KClass<*>,
    walker: Walker<Any?>,
    escapeUnicode: Boolean,
    showFieldNames: Boolean
  ): Iterator<Tree>
  {
    val props = cls.dataClassProperties()
    val productIterator = props.asSequence().map { it.invoke(x) }
    val productElementNames = props.asSequence().map { it.name }

    return if (!showFieldNames)
      productIterator.map { x -> walker.treeify(x, escapeUnicode, showFieldNames) }.iterator()
    else
      productElementNames
        .withIndex()
        .map { (i, name) ->
          val elem = props[i].invoke(x)
          Tree.KeyValue(name, walker.treeify(elem, escapeUnicode, showFieldNames))
        }.iterator()
  }

}
