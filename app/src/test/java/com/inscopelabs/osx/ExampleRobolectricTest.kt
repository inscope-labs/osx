package com.inscopelabs.osx

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.inscopelabs.osx.domain.envelope.EnvelopeFactory
import com.inscopelabs.osx.domain.envelope.EnvelopeType
import com.inscopelabs.osx.domain.envelope.EnvelopeValidator
import com.inscopelabs.osx.domain.key.ConsoleKeyManager
import com.inscopelabs.osx.domain.key.model.KeyRole
import com.inscopelabs.osx.domain.profile.OperationalProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("OSX Console", appName)
  }

  @Test
  fun `verify signed envelope digest and authenticity`() {
    val keyManager = ConsoleKeyManager()
    val signingKey = keyManager.getActiveKey(KeyRole.SIGNING)
    val validator = EnvelopeValidator()
    val factory = EnvelopeFactory(validator)

    val envelope = factory.createEnvelope(
      type = EnvelopeType.HANDSHAKE,
      senderId = "OP-TEST",
      recipientId = "CTRL-CORE-01",
      payloadRaw = "{\"test\":true}",
      profile = OperationalProfile.BASIC,
      signerKey = signingKey
    )

    val validationResult = validator.validateEnvelope(envelope)
    assertTrue("Envelope with signing key should be valid", validationResult.isSuccess)
  }

  @Test
  fun `verify heartbeat envelope without signer`() {
    val validator = EnvelopeValidator()
    val factory = EnvelopeFactory(validator)

    val heartbeat = factory.createEnvelope(
      type = EnvelopeType.HEARTBEAT,
      senderId = "OP-TEST",
      recipientId = "CTRL-CORE-01",
      payloadRaw = "{\"ping\":true}",
      profile = OperationalProfile.BASIC
    )

    val validationResult = validator.validateEnvelope(heartbeat)
    assertTrue("Heartbeat envelope should be valid", validationResult.isSuccess)
  }
}
