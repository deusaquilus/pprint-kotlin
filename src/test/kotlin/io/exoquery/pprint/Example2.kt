package io.exoquery.pprint

import io.exoquery.fansi.Str
import io.kotest.assertions.fail
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

@Serializable
data class SerName(val first: String, val last: String)

@Serializable
data class SerPerson(val name: SerName, val age: Int)

@OptIn(ExperimentalSerializationApi::class)
fun main() {
  val p = SerPerson(SerName("Joe", "Bloggs"), 123)

  //val serDesc: SerialDescriptor = SerPerson.serializer().descriptor
  val serializer: KSerializer<SerPerson> = SerPerson.serializer()
  val encoder = TreeElementEncoder(true, true)
  serializer.serialize(encoder, p)
  val tree = encoder.retrieve()
  val str = Str.joinSafe(TreeRenderer.makeStr(tree, PrintSettings.Defaults))

  println(str)
}