/*
 * Copyright (c) 2020 Hai Zhang <dreaming.in.code.zh@gmail.com>
 * All Rights Reserved.
 */

package me.zhanghai.android.files.storage

import android.os.Parcelable
import java8.nio.file.Path

interface Storage : Parcelable {
    val id: Long
    val name: String
    val description: String
    val path: Path
}
