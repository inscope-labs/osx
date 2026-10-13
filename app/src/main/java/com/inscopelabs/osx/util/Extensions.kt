package com.inscopelabs.osx.util

import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

fun ByteArray.toHexString(): String = joinToString("") { "%02x".format(it) }

fun String.sha256(): String {
    val digest = MessageDigest.getInstance("SHA-256")
    val hash = digest.digest(this.toByteArray(Charsets.UTF_8))
    return hash.toHexString()
}

fun Long.formatEpoch(): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
    return sdf.format(Date(this))
}

fun Long.formatEpochShort(): String {
    val sdf = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
    return sdf.format(Date(this))
}

fun generateId(prefix: String = "id"): String {
    val raw = UUID.randomUUID().toString().replace("-", "").take(12)
    return "$prefix-$raw"
}
