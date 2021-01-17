/*
 * Copyright (c) 2021 Hai Zhang <dreaming.in.code.zh@gmail.com>
 * All Rights Reserved.
 */

package me.zhanghai.android.files.storage

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import me.zhanghai.android.files.R
import me.zhanghai.android.files.databinding.AddLanSmbServerFragmentBinding
import me.zhanghai.android.files.ui.StaticAdapter
import me.zhanghai.android.files.util.Failure
import me.zhanghai.android.files.util.Loading
import me.zhanghai.android.files.util.Stateful
import me.zhanghai.android.files.util.createIntent
import me.zhanghai.android.files.util.fadeToVisibilityUnsafe
import me.zhanghai.android.files.util.finish
import me.zhanghai.android.files.util.putArgs
import me.zhanghai.android.files.util.startActivityForResultSafe
import me.zhanghai.android.files.util.viewModels

class AddLanSmbServerFragment : Fragment() {
    private val viewModel by viewModels { { AddLanSmbServerViewModel() } }

    private lateinit var binding: AddLanSmbServerFragmentBinding

    private lateinit var loadingAdapter: StaticAdapter
    private lateinit var serverListAdapter: LanSmbServerListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View =
        AddLanSmbServerFragmentBinding.inflate(inflater, container, false)
            .also { binding = it }
            .root

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val activity = requireActivity() as AppCompatActivity
        activity.setSupportActionBar(binding.toolbar)

        binding.swipeRefreshLayout.setOnRefreshListener { viewModel.reload() }
        binding.recyclerView.layoutManager = LinearLayoutManager(activity)
        loadingAdapter = StaticAdapter(R.layout.lan_smb_server_loading_item)
        serverListAdapter = LanSmbServerListAdapter { addSmbServer(it) }
        val addAdapter = StaticAdapter(R.layout.lan_smb_server_add_item) { addSmbServer(null) }
        binding.recyclerView.adapter = ConcatAdapter(
            ConcatAdapter.Config.Builder()
                .setIsolateViewTypes(true)
                .setStableIdMode(ConcatAdapter.Config.StableIdMode.ISOLATED_STABLE_IDS)
                .build(), loadingAdapter, serverListAdapter, addAdapter
        )

        viewModel.lanSmbServerListLiveData.observe(viewLifecycleOwner) {
            onLanSmbServerListChanged(it)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
        when (item.itemId) {
            android.R.id.home -> {
                // This recreates MainActivity but we cannot have singleTop as launch mode along
                // with document launch mode.
                //AppCompatActivity activity = (AppCompatActivity) requireActivity();
                //activity.onSupportNavigateUp();
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            REQUEST_CODE_ADD_SMB_SERVER -> {
                if (resultCode == Activity.RESULT_OK) {
                    finish()
                }
            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun onLanSmbServerListChanged(stateful: Stateful<List<LanSmbServer>>) {
        if (stateful is Failure) {
            stateful.throwable.printStackTrace()
        }
        val isLoading = stateful is Loading
        binding.swipeRefreshLayout.isEnabled = !isLoading
        binding.swipeRefreshLayout.isRefreshing = false
        binding.progress.fadeToVisibilityUnsafe(isLoading)
        val servers = stateful.value ?: emptyList()
        loadingAdapter.itemCount = if (isLoading && servers.isEmpty()) 1 else 0
        serverListAdapter.replace(servers)
    }

    private fun addSmbServer(server: LanSmbServer?) {
        startActivityForResultSafe(
            EditSmbServerActivity::class.createIntent()
                .putArgs(EditSmbServerFragment.Args(host = server?.host)),
            REQUEST_CODE_ADD_SMB_SERVER
        )
    }

    companion object {
        private const val REQUEST_CODE_ADD_SMB_SERVER = 1
    }
}
