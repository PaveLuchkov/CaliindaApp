package com.lpavs.caliinda.core.data.remote.agent

import android.util.Log
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
                try {
                    jsonDecoder.json.decodeFromJsonElement(StructuredResponse.serializer(), jsonElement)
                } catch (e: Exception) {
                    Log.e("ChatResponseSerializer", "Failed to decode StructuredResponse", e)
                    error("Failed to parse structured response object: ${e.message}")
                }
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

object PreviewTypeSerializer : KSerializer<PreviewType> {
    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("PreviewType")

    override fun deserialize(decoder: Decoder): PreviewType {
        val jsonDecoder = decoder as? JsonDecoder
            ?: error("Can be deserialized only by Json")
        val jsonObject = jsonDecoder.decodeJsonElement().jsonObject

        // Получаем единственный ключ из объекта ("search", "update" и т.д.)
        val typeKey = jsonObject.keys.first()

        return when (typeKey) {
            "search" -> jsonDecoder.json.decodeFromJsonElement(
                PreviewType.Search.serializer(),
                jsonObject
            )
            "update" -> jsonDecoder.json.decodeFromJsonElement(
                PreviewType.Update.serializer(),
                jsonObject
            )
            "create" -> jsonDecoder.json.decodeFromJsonElement(
                PreviewType.Create.serializer(),
                jsonObject
            )
            "delete" -> jsonDecoder.json.decodeFromJsonElement(
                PreviewType.Delete.serializer(),
                jsonObject
            )
            else -> throw IllegalArgumentException("Unknown preview type: $typeKey")
        }
    }

    override fun serialize(encoder: Encoder, value: PreviewType) {
        // Сериализация пока не нужна, но лучше реализовать для полноты
        val jsonEncoder = encoder as? JsonEncoder
            ?: error("Can be serialized only by Json")

        when (value) {
            is PreviewType.Search -> jsonEncoder.encodeSerializableValue(PreviewType.Search.serializer(), value)
            is PreviewType.Update -> jsonEncoder.encodeSerializableValue(PreviewType.Update.serializer(), value)
            is PreviewType.Create -> jsonEncoder.encodeSerializableValue(PreviewType.Create.serializer(), value)
            is PreviewType.Delete -> jsonEncoder.encodeSerializableValue(PreviewType.Delete.serializer(), value)
        }
    }
}
