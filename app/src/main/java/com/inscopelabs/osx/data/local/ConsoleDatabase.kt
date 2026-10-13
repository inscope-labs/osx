package com.inscopelabs.osx.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

// ----------------- ENTITIES -----------------

@Entity(tableName = "envelopes")
data class EnvelopeEntity(
    @PrimaryKey val id: String,
    val type: String,
    val senderId: String,
    val recipientId: String,
    val timestamp: Long,
    val nonce: String,
    val profile: String,
    val signerKeyId: String?,
    val payloadRaw: String,
    val digest: String,
    val signature: String?,
    val status: String
)

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey val sessionId: String,
    val operatorId: String,
    val targetControllerId: String,
    val profile: String,
    val state: String,
    val sessionKeyId: String,
    val startedAt: Long,
    val expiresAt: Long,
    val lastHeartbeatAt: Long,
    val handoffTargetConsoleId: String?
)

@Entity(tableName = "runtimes")
data class RuntimeEntity(
    @PrimaryKey val id: String,
    val name: String,
    val executionEngine: String,
    val endpointUrl: String,
    val status: String,
    val supportedProfilesCsv: String,
    val maxConcurrentOrders: Int,
    val currentActiveOrders: Int,
    val lastSeenAt: Long
)

@Entity(tableName = "audit_records")
data class AuditEntity(
    @PrimaryKey val id: String,
    val targetId: String,
    val envelopeType: String,
    val passed: Boolean,
    val details: String,
    val timestamp: Long
)

// ----------------- DAOS -----------------

@Dao
interface EnvelopeDao {
    @Query("SELECT * FROM envelopes ORDER BY timestamp DESC")
    fun getAllEnvelopes(): Flow<List<EnvelopeEntity>>

    @Query("SELECT * FROM envelopes WHERE id = :id LIMIT 1")
    suspend fun getEnvelopeById(id: String): EnvelopeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEnvelope(envelope: EnvelopeEntity)

    @Query("DELETE FROM envelopes WHERE timestamp < :cutoff")
    suspend fun pruneOldEnvelopes(cutoff: Long)
}

@Dao
interface SessionDao {
    @Query("SELECT * FROM sessions ORDER BY startedAt DESC")
    fun getAllSessions(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE state = 'ACTIVE' LIMIT 1")
    suspend fun getActiveSession(): SessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: SessionEntity)
}

@Dao
interface RuntimeDao {
    @Query("SELECT * FROM runtimes")
    fun getAllRuntimes(): Flow<List<RuntimeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRuntime(runtime: RuntimeEntity)

    @Query("UPDATE runtimes SET status = :status, lastSeenAt = :timestamp WHERE id = :id")
    suspend fun updateStatus(id: String, status: String, timestamp: Long)
}

@Dao
interface AuditDao {
    @Query("SELECT * FROM audit_records ORDER BY timestamp DESC LIMIT 100")
    fun getRecentAudits(): Flow<List<AuditEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAudit(audit: AuditEntity)
}

// ----------------- DATABASE -----------------

@Database(
    entities = [
        EnvelopeEntity::class,
        SessionEntity::class,
        RuntimeEntity::class,
        AuditEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class ConsoleDatabase : RoomDatabase() {
    abstract fun envelopeDao(): EnvelopeDao
    abstract fun sessionDao(): SessionDao
    abstract fun runtimeDao(): RuntimeDao
    abstract fun auditDao(): AuditDao

    companion object {
        @Volatile
        private var INSTANCE: ConsoleDatabase? = null

        fun getInstance(context: Context): ConsoleDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ConsoleDatabase::class.java,
                    "osx_console.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
