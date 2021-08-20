package ipogudin.jackson

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.toPersistentMap

class PersitentMapDeserializer<K, V>: StdDeserializer<PersistentMap<K, V>>(PersistentMap::class.java) {

    override fun deserialize(parser: JsonParser?, context: DeserializationContext?): PersistentMap<K, V> =
        parser?.readValueAs(Map::class.java)?.toPersistentMap() as PersistentMap<K, V>

}