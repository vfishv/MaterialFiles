/*
 * Copyright (c) 2018 Hai Zhang <dreaming.in.code.zh@gmail.com>
 * All Rights Reserved.
 */

package me.zhanghai.android.files.storage

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.appcompat.app.AppCompatDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import me.zhanghai.android.files.R
import me.zhanghai.android.files.util.createIntent
import me.zhanghai.android.files.util.putArgs

class AddStorageDialogFragment : AppCompatDialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        MaterialAlertDialogBuilder(requireContext(), theme)
            .setTitle(R.string.storage_add_storage_title)
            .apply {
                val items = STORAGE_TYPES.map { getString(it.first) }.toTypedArray<CharSequence>()
                setItems(items) { _, which ->
                    startActivity(STORAGE_TYPES[which].second)
                    dismiss()
                }
            }
            .create()

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)

        requireActivity().finish()
    }

    companion object {
        private val STORAGE_TYPES = listOf(
            R.string.storage_add_storage_document_tree
                to AddDocumentTreeActivity::class.createIntent(),
            R.string.storage_add_storage_smb_server
                to EditSmbServerActivity::class.createIntent()
                    .putArgs(EditSmbServerFragment.Args(null))
        )
    }
}
