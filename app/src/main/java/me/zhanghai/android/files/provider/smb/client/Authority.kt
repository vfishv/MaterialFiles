/*
 * Copyright (c) 2020 Hai Zhang <dreaming.in.code.zh@gmail.com>
 * All Rights Reserved.
 */

package me.zhanghai.android.files.provider.smb.client

import android.os.Parcelable
import com.hierynomus.smbj.SMBClient
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Authority(
    val host: String,
    val port: Int
) : Parcelable {
    override fun toString(): String = if (port != SMBClient.DEFAULT_PORT) "$host:$port" else host
}
