/*
 * Copyright 2014-2020 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.client.features.json.tests

import io.ktor.client.call.*
import io.ktor.client.features.json.serializer.*
import io.ktor.utils.io.streams.*
import kotlinx.serialization.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.*
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.*
import kotlin.test.*

class KotlinxSerializerTest {
    @OptIn(ImplicitReflectionSerializer::class)
    @Test
    fun testCustomDeserializer() {
        val upwrapper = indexListUnwrapper<TestEntry>()

        @OptIn(UnstableDefault::class)
        val serializer = Json {
            ignoreUnknownKeys = true
            serialModule = serializersModule(upwrapper)
        }

        val kotlinxSerializer = KotlinxSerializer(serializer)
        val json = """
            {
                "something": "something",
                "data": [
                    {"a": "hello", "b": 42},
                    {"a": "bye", "b": 4242}
                ]
            }
        """.trimIndent().byteInputStream().asInput()

        @Suppress("UNCHECKED_CAST")
        val data = kotlinxSerializer.read(typeInfo<List<TestEntry>>(), json) as List<TestEntry>

        assertEquals(2, data.size)
        assertEquals(TestEntry("hello", 42), data[0])
        assertEquals(TestEntry("bye", 4242), data[1])
    }
}

@Serializable
data class TestEntry(val a: String, val b: Int)

@ImplicitReflectionSerializer
inline fun <reified T> indexListUnwrapper() =
    object : JsonTransformingSerializer<List<T>>(serializer<T>().list, "unwrap") {
        override fun readTransform(element: JsonElement): JsonElement {
            return if (element is JsonArray) element else element.jsonObject.values.firstOrNull { it is JsonArray }
                ?: error("Collection not found in json")
        }
    }
