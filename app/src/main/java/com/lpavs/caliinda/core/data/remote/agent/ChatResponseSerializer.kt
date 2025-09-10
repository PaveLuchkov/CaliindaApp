package com.lpavs.caliinda.core.data.remote.agent

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object ChatApiResponseSerializer : KSerializer<ChatApiResponse> {
  override val descriptor: SerialDescriptor = buildClassSerialDescriptor("ChatApiResponse")

  override fun deserialize(decoder: Decoder): ChatApiResponse {
    val jsonDecoder = decoder as? JsonDecoder ?: error("This serializer can be used only with Json format")
    val jsonObject = jsonDecoder.decodeJsonElement().jsonObject

    val agentName = jsonObject["agent"]?.jsonPrimitive?.content
      ?: throw Exception("Agent field is missing or not a string")

    val responseElement = jsonObject["response"]
      ?: throw Exception("Response field is missing")

    val responseObject: Any = when (agentName) {
      "MAIN_Agent", "Waiter_Action", "Planner" -> {
        responseElement.jsonPrimitive.content
      }
      "PresentationLayer" -> {
          jsonDecoder.json.decodeFromJsonElement(StructuredResponse.serializer(), responseElement)
      }
      "TacticAgent", "StrategyAgent" -> {
        val responseJsonObj = responseElement.jsonObject
        val planType = responseJsonObj["response_type"]?.jsonPrimitive?.content

        when (planType) {
          "days_plan" -> jsonDecoder.json.decodeFromJsonElement(DaysPlanResponse.serializer(), responseJsonObj)
          "suggestion_plan" -> jsonDecoder.json.decodeFromJsonElement(SuggestionPlanResponse.serializer(), responseJsonObj)
          else -> throw Exception("Unknown plan type '$planType' for agent '$agentName'")
        }
      }

      else -> throw Exception("Unknown agent type: $agentName")
    }

    return ChatApiResponse(agent = agentName, response = responseObject)
  }

  override fun serialize(encoder: Encoder, value: ChatApiResponse) {
  }
}

object PreviewTypeSerializer : KSerializer<PreviewType> {
  override val descriptor: SerialDescriptor = buildClassSerialDescriptor("PreviewType")

  override fun deserialize(decoder: Decoder): PreviewType {
    val jsonDecoder = decoder as? JsonDecoder ?: error("Can be deserialized only by Json")
    val jsonObject = jsonDecoder.decodeJsonElement().jsonObject

    val typeKey = jsonObject.keys.first()

    return when (typeKey) {
      "search" ->
          jsonDecoder.json.decodeFromJsonElement(PreviewType.Search.serializer(), jsonObject)
      "update" ->
          jsonDecoder.json.decodeFromJsonElement(PreviewType.Update.serializer(), jsonObject)
      "create" ->
          jsonDecoder.json.decodeFromJsonElement(PreviewType.Create.serializer(), jsonObject)
      "delete" ->
          jsonDecoder.json.decodeFromJsonElement(PreviewType.Delete.serializer(), jsonObject)
      else -> throw IllegalArgumentException("Unknown preview type: $typeKey")
    }
  }

  override fun serialize(encoder: Encoder, value: PreviewType) {
    val jsonEncoder = encoder as? JsonEncoder ?: error("Can be serialized only by Json")

    when (value) {
      is PreviewType.Search ->
          jsonEncoder.encodeSerializableValue(PreviewType.Search.serializer(), value)
      is PreviewType.Update ->
          jsonEncoder.encodeSerializableValue(PreviewType.Update.serializer(), value)
      is PreviewType.Create ->
          jsonEncoder.encodeSerializableValue(PreviewType.Create.serializer(), value)
      is PreviewType.Delete ->
          jsonEncoder.encodeSerializableValue(PreviewType.Delete.serializer(), value)
    }
  }
}
