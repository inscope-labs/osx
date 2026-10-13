package com.inscopelabs.osx.data.local

import com.inscopelabs.osx.domain.envelope.Envelope
import com.inscopelabs.osx.domain.envelope.EnvelopeHeader
import com.inscopelabs.osx.domain.envelope.EnvelopeStatus
import com.inscopelabs.osx.domain.envelope.EnvelopeType
import com.inscopelabs.osx.domain.profile.OperationalProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

/**
 * High-performance envelope cache bridging memory and local database.
 */
class EnvelopeCache(
    private val database: ConsoleDatabase? = null
) {
    private val memoryStore = ConcurrentHashMap<String, Envelope>()
    private val _cachedEnvelopes = MutableStateFlow<List<Envelope>>(emptyList())
    val cachedEnvelopes: StateFlow<List<Envelope>> = _cachedEnvelopes.asStateFlow()

    fun put(envelope: Envelope) {
        memoryStore[envelope.header.id] = envelope
        _cachedEnvelopes.value = memoryStore.values.sortedByDescending { it.header.timestamp }
    }

    fun get(id: String): Envelope? {
        return memoryStore[id]
    }

    fun contains(id: String): Boolean {
        return memoryStore.containsKey(id)
    }

    fun clear() {
        memoryStore.clear()
        _cachedEnvelopes.value = emptyList()
    }

    fun toEntity(envelope: Envelope): EnvelopeEntity {
        return EnvelopeEntity(
            id = envelope.header.id,
            type = envelope.header.type.name,
            senderId = envelope.header.senderId,
            recipientId = envelope.header.recipientId,
            timestamp = envelope.header.timestamp,
            nonce = envelope.header.nonce,
            profile = envelope.header.profile.name,
            signerKeyId = envelope.header.signerKeyId,
            payloadRaw = envelope.payloadRaw,
            digest = envelope.digest,
            signature = envelope.signature,
            status = envelope.status.name
        )
    }

    fun toDomain(entity: EnvelopeEntity): Envelope {
        return Envelope(
            header = EnvelopeHeader(
                id = entity.id,
                type = EnvelopeType.valueOf(entity.type),
                senderId = entity.senderId,
                recipientId = entity.recipientId,
                timestamp = entity.timestamp,
                nonce = entity.nonce,
                profile = OperationalProfile.valueOf(entity.profile),
                signerKeyId = entity.signerKeyId
            ),
            payloadRaw = entity.payloadRaw,
            digest = entity.digest,
            signature = entity.signature,
            status = EnvelopeStatus.valueOf(entity.status)
        )
    }
}
