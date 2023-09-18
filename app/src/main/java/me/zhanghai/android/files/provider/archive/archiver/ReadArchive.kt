/*
 * Copyright (c) 2023 Hai Zhang <dreaming.in.code.zh@gmail.com>
 * All Rights Reserved.
 */

package me.zhanghai.android.files.provider.archive.archiver

import android.system.OsConstants
import java8.nio.channels.SeekableByteChannel
import java8.nio.charset.StandardCharsets
import java8.nio.file.attribute.FileTime
import me.zhanghai.android.files.provider.common.PosixFileMode
import me.zhanghai.android.files.provider.common.PosixFileModeBit
import me.zhanghai.android.files.provider.common.PosixFileType
import me.zhanghai.android.files.provider.common.PosixGroup
import me.zhanghai.android.files.provider.common.PosixUser
import me.zhanghai.android.files.provider.common.toByteString
import me.zhanghai.android.libarchive.Archive
import me.zhanghai.android.libarchive.ArchiveEntry
import me.zhanghai.android.libarchive.ArchiveException
import org.threeten.bp.Instant
import java.io.Closeable
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.charset.Charset

class ReadArchive : Closeable {
    private val archive = Archive.readNew()

    @Throws(ArchiveException::class)
    constructor(inputStream: InputStream) {
        var successful = false
        try {
            Archive.setCharset(archive, StandardCharsets.UTF_8.name().toByteArray())
            Archive.readSupportFilterAll(archive)
            Archive.readSupportFormatAll(archive)
            Archive.readSetCallbackData(archive, null)
            val buffer = ByteBuffer.allocate(DEFAULT_BUFFER_SIZE)
            Archive.readSetReadCallback<Any?>(archive) { _, _ ->
                buffer.clear()
                val bytesRead = try {
                    inputStream.read(buffer.array())
                } catch (e: IOException) {
                    throw ArchiveException(Archive.ERRNO_FATAL, "InputStream.read", e)
                }
                if (bytesRead != -1) {
                    buffer.limit(bytesRead)
                    buffer
                } else {
                    null
                }
            }
            Archive.readSetSkipCallback<Any?>(archive) { _, _, request ->
                try {
                    inputStream.skip(request)
                } catch (e: IOException) {
                    throw ArchiveException(Archive.ERRNO_FATAL, "InputStream.skip", e)
                }
            }
            Archive.readOpen1(archive)
            successful = true
        } finally {
            if (!successful) {
                close()
            }
        }
    }

    @Throws(ArchiveException::class)
    constructor(channel: SeekableByteChannel) {
        var successful = false
        try {
            Archive.setCharset(archive, StandardCharsets.UTF_8.name().toByteArray())
            Archive.readSupportFilterAll(archive)
            Archive.readSupportFormatAll(archive)
            Archive.readSetCallbackData(archive, null)
            val buffer = ByteBuffer.allocateDirect(DEFAULT_BUFFER_SIZE)
            Archive.readSetReadCallback<Any?>(archive) { _, _ ->
                buffer.clear()
                val bytesRead = try {
                    channel.read(buffer)
                } catch (e: IOException) {
                    throw ArchiveException(Archive.ERRNO_FATAL, "SeekableByteChannel.read", e)
                }
                if (bytesRead != -1) {
                    buffer.flip()
                    buffer
                } else {
                    null
                }
            }
            Archive.readSetSkipCallback<Any?>(archive) { _, _, request ->
                try {
                    channel.position(channel.position() + request)
                } catch (e: IOException) {
                    throw ArchiveException(Archive.ERRNO_FATAL, "SeekableByteChannel.position", e)
                }
                request
            }
            Archive.readSetSeekCallback<Any?>(archive) { _, _, offset, whence ->
                val newPosition: Long
                try {
                    newPosition = when (whence) {
                        OsConstants.SEEK_SET -> offset
                        OsConstants.SEEK_CUR -> channel.position() + offset
                        OsConstants.SEEK_END -> channel.size() + offset
                        else -> throw ArchiveException(
                            Archive.ERRNO_FATAL,
                            "Unknown whence $whence"
                        )
                    }
                    channel.position(newPosition)
                } catch (e: IOException) {
                    throw ArchiveException(Archive.ERRNO_FATAL, "SeekableByteChannel.position", e)
                }
                newPosition
            }
            Archive.readOpen1(archive)
            successful = true
        } finally {
            if (!successful) {
                close()
            }
        }
    }

    @Throws(ArchiveException::class)
    fun readEntry(charset: Charset): Entry? {
        val entry = Archive.readNextHeader(archive)
        if (entry == 0L) {
            return null
        }
        val name =
            getEntryString(ArchiveEntry.pathnameUtf8(entry), ArchiveEntry.pathname(entry), charset)
                ?: throw ArchiveException(
                    Archive.ERRNO_FATAL, "pathname == null && pathnameUtf8 == null"
                )
        val isEncrypted = ArchiveEntry.isEncrypted(entry)
        val stat = ArchiveEntry.stat(entry)
        val lastModifiedTime = if (ArchiveEntry.mtimeIsSet(entry)) {
            FileTime.from(
                Instant.ofEpochSecond(stat.stMtim.tvSec, stat.stMtim.tvNsec)
            )
        } else {
            null
        }
        val lastAccessTime = if (ArchiveEntry.atimeIsSet(entry)) {
            FileTime.from(
                Instant.ofEpochSecond(stat.stAtim.tvSec, stat.stAtim.tvNsec)
            )
        } else {
            null
        }
        val creationTime = if (ArchiveEntry.birthtimeIsSet(entry)) {
            FileTime.from(
                Instant.ofEpochSecond(
                    ArchiveEntry.birthtime(entry), ArchiveEntry.birthtimeNsec(entry)
                )
            )
        } else {
            null
        }
        val type = PosixFileType.fromMode(stat.stMode)
        val size = stat.stSize
        // TODO: There's no way to know if UID/GID is unset or root.
        val owner = PosixUser(
            stat.stUid, getEntryString(
                ArchiveEntry.unameUtf8(entry), ArchiveEntry.uname(entry), charset
            )?.toByteString()
        )
        val group = PosixGroup(
            stat.stGid, getEntryString(
                ArchiveEntry.gnameUtf8(entry), ArchiveEntry.gname(entry), charset
            )?.toByteString()
        )
        val mode = PosixFileMode.fromInt(stat.stMode)
        val symbolicLinkTarget =
            getEntryString(ArchiveEntry.symlinkUtf8(entry), ArchiveEntry.symlink(entry), charset)
        return Entry(
            name, isEncrypted, lastModifiedTime, lastAccessTime, creationTime, type, size, owner,
            group, mode, symbolicLinkTarget
        )
    }

    private fun getEntryString(stringUtf8: String?, string: ByteArray?, charset: Charset): String? =
        stringUtf8 ?: string?.toString(charset)

    fun hasEncryptedEntries(): Boolean? {
        val hasEncryptedEntries = Archive.readHasEncryptedEntries(archive)
        return when {
            hasEncryptedEntries > 0 -> true
            hasEncryptedEntries == 0 -> false
            else -> null
        }
    }

    @Throws(ArchiveException::class)
    fun newDataInputStream(): InputStream = DataInputStream()

    @Throws(ArchiveException::class)
    fun addPassword(password: String) {
        Archive.readAddPassphrase(archive, password.toByteArray())
    }

    @Throws(ArchiveException::class)
    override fun close() {
        Archive.readFree(archive)
    }

    class Entry(
        val name: String,
        val isEncrypted: Boolean,
        val lastModifiedTime: FileTime?,
        val lastAccessTime: FileTime?,
        val creationTime: FileTime?,
        val type: PosixFileType,
        val size: Long,
        val owner: PosixUser?,
        val group: PosixGroup?,
        val mode: Set<PosixFileModeBit>,
        val symbolicLinkTarget: String?
    ) {
        val isDirectory: Boolean
            get() = type == PosixFileType.DIRECTORY
    }

    private inner class DataInputStream : InputStream() {
        private val oneByteBuffer = ByteBuffer.allocateDirect(1)

        @Throws(IOException::class)
        override fun read(): Int {
            read(oneByteBuffer)
            return if (oneByteBuffer.hasRemaining()) oneByteBuffer.get().toUByte().toInt() else -1
        }

        @Throws(IOException::class)
        override fun read(b: ByteArray, off: Int, len: Int): Int {
            val buffer = ByteBuffer.wrap(b, off, len)
            read(buffer)
            return if (buffer.hasRemaining()) buffer.remaining() else -1
        }

        @Throws(IOException::class)
        private fun read(buffer: ByteBuffer) {
            buffer.clear()
            Archive.readData(archive, buffer)
            buffer.flip()
        }
    }
}
