package com.oplus.groupimaging.core

import java.security.MessageDigest

internal fun sha256Hex(value: String): String =
    MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray())
        .joinToString(separator = "") { byte -> "%02x".format(byte) }
