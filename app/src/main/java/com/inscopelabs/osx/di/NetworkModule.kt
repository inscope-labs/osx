package com.inscopelabs.osx.di

import com.inscopelabs.osx.data.network.ControllerTransport
import com.inscopelabs.osx.data.network.DiscoveryClient
import com.inscopelabs.osx.data.network.EnvelopeSerializer

class NetworkModule {
    val envelopeSerializer: EnvelopeSerializer by lazy { EnvelopeSerializer() }
    val controllerTransport: ControllerTransport by lazy { ControllerTransport(envelopeSerializer) }
    val discoveryClient: DiscoveryClient by lazy { DiscoveryClient() }
}
