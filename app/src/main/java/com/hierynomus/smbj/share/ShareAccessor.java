/*
 * Copyright (c) 2020 Hai Zhang <dreaming.in.code.zh@gmail.com>
 * All Rights Reserved.
 */

package com.hierynomus.smbj.share;

import com.hierynomus.mssmb2.SMB2FileId;
import com.hierynomus.mssmb2.messages.SMB2IoctlResponse;
import com.hierynomus.smbj.io.ArrayByteChunkProvider;

import androidx.annotation.NonNull;

public class ShareAccessor {
    /**
     * @see Share#ioctl(com.hierynomus.mssmb2.SMB2FileId, long, boolean,
     *      com.hierynomus.smbj.io.ByteChunkProvider, int)
     */
    @NonNull
    public static SMB2IoctlResponse ioctl(@NonNull Share share, @NonNull SMB2FileId fileId,
                                          long ctlCode, boolean isFsCtl, @NonNull byte[] inData,
                                          int inOffset, int inLength,
                                          @NonNull StatusHandler statusHandler, long timeout) {
        return share.receive(share.ioctlAsync(fileId, ctlCode, isFsCtl, new ArrayByteChunkProvider(
                inData, inOffset, inLength, 0), -1), "IOCTL", fileId, statusHandler, timeout);
    }
}
