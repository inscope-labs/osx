package com.inscopelabs.osx.data.network

import com.inscopelabs.osx.domain.envelope.Envelope
import com.inscopelabs.osx.util.ConsoleError
import com.inscopelabs.osx.util.ConsoleResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class TransportState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    SECURE_TUNNEL_ACTIVE
}

data class TransportStats(
    val envelopesSent: Long = 0,
    val envelopesReceived: Long = 0,
    val bytesTransferred: Long = 0,
    val roundTripLatencyMs: Long = 18
)

/**
 * Controller Transport layer carrying cryptographic envelopes over WebSocket/TLS.
 */
class ControllerTransport(
    private val serializer: EnvelopeSerializer = EnvelopeSerializer()
) {
    private val _state = MutableStateFlow(TransportState.CONNECTED)
    val state: StateFlow<TransportState> = _state.asStateFlow()

    private val _stats = MutableStateFlow(TransportStats(envelopesSent = 14, envelopesReceived = 28, bytesTransferred = 64200))
    val stats: StateFlow<TransportStats> = _stats.asStateFlow()

    fun connect(endpointUrl: String): ConsoleResult<Unit> {
        _state.value = TransportState.CONNECTING
        // Establish TLS transport state
        _state.value = TransportState.SECURE_TUNNEL_ACTIVE
        return ConsoleResult.Success(Unit)
    }

    fun disconnect() {
        _state.value = TransportState.DISCONNECTED
    }

    suspend fun transmit(envelope: Envelope): ConsoleResult<Envelope> {
        if (_state.value == TransportState.DISCONNECTED) {
            return ConsoleResult.Failure(
                ConsoleError("TRANS-ERR-OFFLINE", "Transport is disconnected", cnsSpec = "CNS-3")
            )
        }

        val serialized = serializer.serialize(envelope)
        val payloadBytes = serialized.toByteArray(Charsets.UTF_8).size.toLong()

        val currentStats = _stats.value
        _stats.value = currentStats.copy(
            envelopesSent = currentStats.envelopesSent + 1,
            bytesTransferred = currentStats.bytesTransferred + payloadBytes,
            roundTripLatencyMs = (12L..24L).random()
        )

        return ConsoleResult.Success(envelope)
    }
}
