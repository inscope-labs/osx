# OS/10 Operator Console (`osx-console`)

The **OS/10 Operator Console** is a high-assurance Android client engineered by In-Scope Labs. It implements the complete CNS-1 through CNS-7 console protocol specifications for secure operator session anchoring, cryptographic envelope validation, multi-profile trust boundaries, runtime discovery, and work order dispatching.

---

## 🏛️ Console Architecture & Specification Mapping

| Module / Spec | Title | Core Package | Description |
|---|---|---|---|
| **CNS-1** | Foundations & Invariants | `com.inscopelabs.osx.domain.foundation` | Core invariants (`INV-01` to `INV-05`) and perimeter trust boundary enforcement (`TrustBoundary`). |
| **CNS-2** | Keys & Authentication | `com.inscopelabs.osx.domain.key` | Key hierarchy (`Root -> Signing -> Session -> Device`), key manager, rotation, and confirmation policies. |
| **CNS-3** | Envelope Interface | `com.inscopelabs.osx.domain.envelope` | Cryptographic envelope format, SHA-256 digests, signatures, factory, and validator. |
| **CNS-4** | Session Lifecycle | `com.inscopelabs.osx.domain.session` | Operator session initiation, state automaton, keepalive heartbeats, and console handoffs. |
| **CNS-5** | Runtimes, Work Orders, Pre-Auth | `com.inscopelabs.osx.domain.runtime` | Execution node registry, work order dispatch, and pre-authorization computational budgets. |
| **CNS-6** | Validation & Revocation | `com.inscopelabs.osx.domain.validation` | Multi-stage `ValidationGateEngine`, state rules, and instant certificate/key revocation list (CRL). |
| **CNS-7** | Operational Profiles | `com.inscopelabs.osx.domain.profile` | Security posture profiles (`BASIC`, `STANDARD`, `HIGH`) with hardware TEE step-up requirements. |

---

## 📂 Project Directory Structure

```text
osx-console/
├── settings.gradle.kts
├── build.gradle.kts
├── gradle.properties
│
├── app/
│   ├── build.gradle.kts
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/inscopelabs/osx/
│       │   │   ├── OsxApplication.kt
│       │   │   ├── MainActivity.kt
│       │   │   │
│       │   │   ├── di/                          # Dependency Injection Container
│       │   │   │   ├── AppModule.kt
│       │   │   │   ├── CryptoModule.kt
│       │   │   │   ├── NetworkModule.kt
│       │   │   │   └── ProfileModule.kt
│       │   │   │
│       │   │   ├── domain/                      # Pure Domain Models & Services
│       │   │   │   ├── envelope/                # CNS-3 Envelope Interface
│       │   │   │   │   ├── Envelope.kt
│       │   │   │   │   ├── EnvelopeType.kt
│       │   │   │   │   ├── EnvelopeValidator.kt
│       │   │   │   │   ├── EnvelopeFactory.kt
│       │   │   │   │   └── model/EnvelopeModels.kt
│       │   │   │   ├── key/                     # CNS-2 Keys & Authentication
│       │   │   │   │   ├── KeyHierarchy.kt
│       │   │   │   │   ├── ConsoleKeyManager.kt
│       │   │   │   │   ├── ConfirmationPolicy.kt
│       │   │   │   │   └── model/ConsoleKey.kt
│       │   │   │   ├── session/                 # CNS-4 Session Lifecycle
│       │   │   │   │   ├── SessionManager.kt
│       │   │   │   │   ├── SessionStart.kt
│       │   │   │   │   ├── HandoffHandler.kt
│       │   │   │   │   └── model/SessionModels.kt
│       │   │   │   ├── runtime/                 # CNS-5 Runtimes, Work Orders, Pre-Auth
│       │   │   │   │   ├── RuntimeRegistry.kt
│       │   │   │   │   ├── WorkOrderManager.kt
│       │   │   │   │   ├── PreAuthorizationManager.kt
│       │   │   │   │   └── model/RuntimeModels.kt
│       │   │   │   ├── validation/              # CNS-6 Validation & Revocation
│       │   │   │   │   ├── ValidationGateEngine.kt
│       │   │   │   │   ├── StateRules.kt
│       │   │   │   │   └── RevocationHandler.kt
│       │   │   │   ├── profile/                 # CNS-7 Operational Profiles
│       │   │   │   │   ├── OperationalProfile.kt  # BASIC, STANDARD, HIGH
│       │   │   │   │   ├── ProfileRequirements.kt
│       │   │   │   │   └── ProfileSelector.kt
│       │   │   │   └── foundation/              # CNS-1 Foundations / Invariants
│       │   │   │       ├── Invariants.kt
│       │   │   │       └── TrustBoundary.kt
│       │   │   │
│       │   │   ├── data/                        # Infrastructure Implementations
│       │   │   │   ├── crypto/
│       │   │   │   │   ├── CryptoProvider.kt
│       │   │   │   │   ├── AndroidKeyStoreProvider.kt
│       │   │   │   │   ├── SoftwareKeyProvider.kt
│       │   │   │   │   └── TeeKeyProvider.kt
│       │   │   │   ├── local/
│       │   │   │   │   ├── ConsoleDatabase.kt       # Room Database & DAOs
│       │   │   │   │   ├── EnvelopeCache.kt
│       │   │   │   │   └── SecureStorage.kt
│       │   │   │   ├── network/
│       │   │   │   │   ├── ControllerTransport.kt
│       │   │   │   │   ├── EnvelopeSerializer.kt
│       │   │   │   │   └── DiscoveryClient.kt
│       │   │   │   └── repository/
│       │   │   │       ├── SessionRepository.kt
│       │   │   │       ├── RuntimeRepository.kt
│       │   │   │       └── AuditRepository.kt
│       │   │   │
│       │   │   ├── ui/                          # Jetpack Compose UI
│       │   │   │   ├── theme/
│       │   │   │   ├── navigation/
│       │   │   │   ├── session/
│       │   │   │   ├── runtime/
│       │   │   │   ├── preauth/
│       │   │   │   ├── settings/
│       │   │   │   ├── envelope/
│       │   │   │   ├── dashboard/
│       │   │   │   ├── audit/
│       │   │   │   └── common/
│       │   │   │
│       │   │   └── util/
│       │   │       ├── Result.kt
│       │   │       └── Extensions.kt
│       │   │
│       │   └── res/
│       │       ├── values/
│       │       └── xml/                         # network security config, backup rules
│
└── README.md
```

---

## 🔒 Security Profiles (CNS-7)

- **BASIC:** Software cryptographic engine (`SoftwareKeyProvider`), simulation transport, development mode.
- **STANDARD:** Hardware-backed `AndroidKeyStoreProvider` (NIST P-256 ECDSA, AES-256-GCM), mandatory operator confirmation.
- **HIGH:** Hardware TEE / StrongBox isolation (`TeeKeyProvider`), biometric step-up authentication, strict replay protection cache.
