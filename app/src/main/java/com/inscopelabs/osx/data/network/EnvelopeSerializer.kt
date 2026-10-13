package com.inscopelabs.osx.data.network

import com.inscopelabs.osx.domain.envelope.Envelope
import com.inscopelabs.osx.domain.envelope.EnvelopeHeader
import com.inscopelabs.osx.domain.envelope.EnvelopeStatus
import com.inscopelabs.osx.domain.envelope.EnvelopeType
import com.inscopelabs.osx.domain.profile.OperationalProfile
import org.json.JSONObject

/**
 * Robust JSON serializer and deserializer for CNS-3 envelopes.
 */
class EnvelopeSerializer {
    fun serialize(envelope: Envelope): String {
        val root = JSONObject()
        val headerObj = JSONObject().apply {
            put("id", envelope.header.id)
            put("type", envelope.header.type.name)
            put("senderId", envelope.header.senderId)
            put("recipientId", envelope.header.recipientId)
            put("timestamp", envelope.header.timestamp)
            put("nonce", envelope.header.nonce)
            put("profile", envelope.header.profile.name)
            if (envelope.header.signerKeyId != null) {
                put("signerKeyId", envelope.header.signerKeyId)
            }
        }
        root.put("header", headerObj)
        root.put("payloadRaw", envelope.payloadRaw)
        root.put("digest", envelope.digest)
        if (envelope.signature != null) {
            root.put("signature", envelope.signature)
        }
        root.put("status", envelope.status.name)
        return root.toString(2)
    }

    fun deserialize(jsonString: String): Envelope {
        val root = JSONObject(jsonString)
        val headerObj = root.getJSONObject("header")
        val header = EnvelopeHeader(
            id = headerObj.getString("id"),
            type = EnvelopeType.valueOf(headerObj.getString("type")),
            senderId = headerObj.getString("senderId"),
            recipientId = headerObj.getString("recipientId"),
            timestamp = headerObj.getLong("timestamp"),
            nonce = headerObj.getString("nonce"),
            profile = OperationalProfile.valueOf(headerObj.getString("profile")),
            signerKeyId = if (headerObj.has("signerKeyId") && !headerObj.isNull("signerKeyId")) headerObj.getString("signerKeyId") else null
        )

        val payloadRaw = root.getString("payloadRaw")
        val digest = root.getString("digest")
        val signature = if (root.has("signature") && !root.isNull("signature")) root.getString("signature") else null
        val status = if (root.has("status")) EnvelopeStatus.valueOf(root.getString("status")) else EnvelopeStatus.UNVERIFIED

        return Envelope(
            header = header,
            payloadRaw = payloadRaw,
            digest = digest,
            signature = signature,
            status = status
        )
    }
}
