package dev.groupimaging.unmark.model

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

class ProfileRepository(
    context: Context,
    profileName: String = "active",
) {
    private val directory = File(context.filesDir, "profiles").apply { mkdirs() }
    private val safeName = profileName.also {
        require(it.matches(Regex("[a-z0-9_-]{1,40}"))) { "Invalid profile name" }
    }
    private val profileFile = File(directory, "$safeName.wmr2")

    fun load(): WatermarkProfile? {
        if (!profileFile.isFile) return null
        return runCatching {
            profileFile.inputStream().use(WatermarkProfileCodec::decode)
        }.getOrNull()
    }

    /**
     * Writes in the same directory, fsyncs, validates by decoding, and only then replaces the
     * active profile. A failed cleanup can never delete a successfully installed profile.
     */
    fun save(profile: WatermarkProfile) {
        val bytes = WatermarkProfileCodec.encode(profile)
        val temp = File(directory, ".$safeName-${System.nanoTime()}.tmp")
        try {
            FileOutputStream(temp).use { output ->
                output.write(bytes)
                output.flush()
                output.fd.sync()
            }

            temp.inputStream().use(WatermarkProfileCodec::decode)

            try {
                Files.move(
                    temp.toPath(),
                    profileFile.toPath(),
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING,
                )
            } catch (_: AtomicMoveNotSupportedException) {
                Files.move(
                    temp.toPath(),
                    profileFile.toPath(),
                    StandardCopyOption.REPLACE_EXISTING,
                )
            }
        } finally {
            // If move succeeded temp no longer exists. If it failed, only the temporary file is
            // removed; the previous active profile is deliberately left untouched.
            temp.delete()
        }
    }

    fun delete() {
        profileFile.delete()
    }
}
