/*
 * Copyright (c) 2018 Hai Zhang <dreaming.in.code.zh@gmail.com>
 * All Rights Reserved.
 */

package me.zhanghai.android.files.navigation

import androidx.lifecycle.MediatorLiveData
import me.zhanghai.android.files.navigation.NavigationItemListLiveData.addSource
import me.zhanghai.android.files.settings.Settings

object NavigationItemListLiveData : MediatorLiveData<List<NavigationItem?>>() {
    private fun loadValue() {
        value = navigationItems
    }

    init {
        // Initialize value before we have any active observer.
        loadValue()
        addSource(StandardDirectoriesLiveData) { loadValue() }
        addSource(Settings.BOOKMARK_DIRECTORIES) { loadValue() }
        addSource(DocumentTreesLiveData) { loadValue() }
    }
}
