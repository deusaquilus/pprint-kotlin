package io.exoquery.pprint

import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties

// Thing passed in must exist but the result could be null which is why its <Any, Any?> in KProperty1
// the first Any is the type of the class, the second is the type of the property
// also note that KProperty1 implements (T) -> R which is this case is (Any) -> Any?
// this is neat because KProperty is a lambda representing a getter
@Suppress("UNCHECKED_CAST")
fun KClass<*>.dataClassProperties(): List<KProperty1<Any, Any?>> {
  val constructorParams = this.constructors.first().parameters.map { it.name }.toSet()
  val props = this.declaredMemberProperties.filter { it.name in constructorParams }

  return props as List<KProperty1<Any, Any?>>
}

object ProductSupport {

  fun treeifyProductElements(
    x: Any,
    cls: KClass<*>,
    walker: Walker,
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
