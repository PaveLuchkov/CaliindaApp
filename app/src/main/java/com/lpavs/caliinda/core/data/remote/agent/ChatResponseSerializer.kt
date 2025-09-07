package com.lpavs.caliinda.core.data.remote.agent

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*

object ChatResponseSerializer : KSerializer<Any> {

    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("ChatResponse")

    override fun deserialize(decoder: Decoder): Any {
        val jsonDecoder = decoder as? JsonDecoder
            ?: error("This serializer can be used only with Json format")

        val jsonElement = jsonDecoder.decodeJsonElement()

        return when (jsonElement) {
            is JsonPrimitive -> {
                if (jsonElement.isString) {
                    jsonElement.content
                } else {
                    error("Unsupported primitive type in response: $jsonElement")
                }
            }

            is JsonObject -> {
                jsonDecoder.json.decodeFromJsonElement(StructuredResponse.serializer(), jsonElement)
            }

            else -> {
                error("Unsupported JSON type in response: $jsonElement")
            }
        }
    }

    override fun serialize(encoder: Encoder, value: Any) {
        val jsonEncoder = encoder as? JsonEncoder
            ?: error("This serializer can be used only with Json format")

        when (value) {
            is String -> jsonEncoder.encodeString(value)
            is StructuredResponse -> jsonEncoder.encodeSerializableValue(StructuredResponse.serializer(), value)
            else -> error("Cannot serialize type ${value::class.simpleName}")
        }
    }
}
