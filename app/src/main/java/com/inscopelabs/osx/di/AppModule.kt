package com.inscopelabs.osx.di

import android.content.Context
import com.inscopelabs.osx.data.local.ConsoleDatabase
import com.inscopelabs.osx.data.local.EnvelopeCache
import com.inscopelabs.osx.data.local.SecureStorage
import com.inscopelabs.osx.data.repository.AuditRepository
import com.inscopelabs.osx.data.repository.RuntimeRepository
import com.inscopelabs.osx.data.repository.SessionRepository
import com.inscopelabs.osx.domain.envelope.EnvelopeFactory
import com.inscopelabs.osx.domain.envelope.EnvelopeValidator
import com.inscopelabs.osx.domain.foundation.TrustBoundary
import com.inscopelabs.osx.domain.runtime.PreAuthorizationManager
import com.inscopelabs.osx.domain.runtime.RuntimeRegistry
import com.inscopelabs.osx.domain.runtime.WorkOrderManager
import com.inscopelabs.osx.domain.session.HandoffHandler
import com.inscopelabs.osx.domain.session.SessionManager
import com.inscopelabs.osx.domain.session.SessionStart
import com.inscopelabs.osx.domain.validation.RevocationHandler
import com.inscopelabs.osx.domain.validation.ValidationGateEngine

/**
 * Top-level application dependency container for OS/10 Console.
 */
class AppModule(val context: Context) {
    val database: ConsoleDatabase by lazy { ConsoleDatabase.getInstance(context) }
    val secureStorage: SecureStorage by lazy { SecureStorage(context) }
    val envelopeCache: EnvelopeCache by lazy { EnvelopeCache(database) }

    val cryptoModule: CryptoModule by lazy { CryptoModule(context) }
    val networkModule: NetworkModule by lazy { NetworkModule() }
    val profileModule: ProfileModule by lazy { ProfileModule(secureStorage) }

    // Foundation & Validation
    val trustBoundary: TrustBoundary by lazy { TrustBoundary() }
    val revocationHandler: RevocationHandler by lazy { RevocationHandler() }
    val envelopeValidator: EnvelopeValidator by lazy { EnvelopeValidator() }
    val envelopeFactory: EnvelopeFactory by lazy { EnvelopeFactory(envelopeValidator) }

    val validationGateEngine: ValidationGateEngine by lazy {
        ValidationGateEngine(
            envelopeValidator = envelopeValidator,
            revocationHandler = revocationHandler,
            trustBoundary = trustBoundary,
            keyManager = cryptoModule.keyManager
        )
    }

    // Sessions
    val sessionStart: SessionStart by lazy { SessionStart(cryptoModule.keyManager) }
    val handoffHandler: HandoffHandler by lazy { HandoffHandler() }
    val sessionManager: SessionManager by lazy { SessionManager(sessionStart, handoffHandler) }

    // Runtimes & Work Orders
    val runtimeRegistry: RuntimeRegistry by lazy { RuntimeRegistry() }
    val preAuthorizationManager: PreAuthorizationManager by lazy { PreAuthorizationManager() }
    val workOrderManager: WorkOrderManager by lazy { WorkOrderManager(preAuthorizationManager) }

    // Repositories
    val sessionRepository: SessionRepository by lazy { SessionRepository(database) }
    val runtimeRepository: RuntimeRepository by lazy { RuntimeRepository(database) }
    val auditRepository: AuditRepository by lazy { AuditRepository(database) }
}
