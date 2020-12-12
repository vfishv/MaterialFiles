/*
 * Copyright (c) 2019 Hai Zhang <dreaming.in.code.zh@gmail.com>
 * All Rights Reserved.
 */

package me.zhanghai.android.files.storage

import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import com.google.android.material.textfield.TextInputEditText
import kotlinx.parcelize.Parcelize
import me.zhanghai.android.files.R
import me.zhanghai.android.files.databinding.EditSmbServerFragmentBinding
import me.zhanghai.android.files.provider.smb.client.Authentication
import me.zhanghai.android.files.provider.smb.client.Authority
import me.zhanghai.android.files.ui.UnfilteredArrayAdapter
import me.zhanghai.android.files.util.Failure
import me.zhanghai.android.files.util.Loading
import me.zhanghai.android.files.util.ParcelableArgs
import me.zhanghai.android.files.util.Stateful
import me.zhanghai.android.files.util.Success
import me.zhanghai.android.files.util.args
import me.zhanghai.android.files.util.fadeToVisibilityUnsafe
import me.zhanghai.android.files.util.getTextArray
import me.zhanghai.android.files.util.hideTextInputLayoutErrorOnTextChange
import me.zhanghai.android.files.util.showToast
import me.zhanghai.android.files.util.takeIfNotEmpty
import me.zhanghai.android.files.util.viewModels

class EditSmbServerFragment : Fragment() {
    private val args by args<Args>()

    private val viewModel by viewModels { { EditSmbServerViewModel() } }

    private lateinit var binding: EditSmbServerFragmentBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View =
        EditSmbServerFragmentBinding.inflate(inflater, container, false)
            .also { binding = it }
            .root

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val activity = requireActivity() as AppCompatActivity
        activity.setSupportActionBar(binding.toolbar)
        activity.setTitle(
            if (args.server != null) {
                R.string.storage_edit_smb_server_title_edit
            } else {
                R.string.storage_edit_smb_server_title_add
            }
        )

        binding.hostEdit.hideTextInputLayoutErrorOnTextChange(binding.hostLayout)
        binding.hostEdit.doAfterTextChanged { updateNamePlaceholder() }
        binding.portEdit.hideTextInputLayoutErrorOnTextChange(binding.portLayout)
        binding.portEdit.doAfterTextChanged { updateNamePlaceholder() }
        binding.authenticationTypeEdit.setAdapter(
            UnfilteredArrayAdapter(
                binding.authenticationTypeEdit.context, R.layout.dropdown_item,
                objects = getTextArray(R.array.storage_edit_smb_server_authentication_type_entries)
            )
        )
        authenticationType = AuthenticationType.PASSWORD
        binding.authenticationTypeEdit.doAfterTextChanged {
            onAuthenticationTypeChanged(authenticationType)
        }
        binding.usernameEdit.hideTextInputLayoutErrorOnTextChange(binding.usernameLayout)
        binding.passwordEdit.hideTextInputLayoutErrorOnTextChange(binding.passwordLayout)
        binding.saveOrConnectAndAddButton.setText(
            if (args.server != null) {
                R.string.save
            } else {
                R.string.storage_edit_smb_server_connect_and_add
            }
        )
        binding.saveOrConnectAndAddButton.setOnClickListener {
            if (args.server != null) {
                saveOrAdd()
            } else {
                connectAndAdd()
            }
        }
        binding.cancelButton.setOnClickListener { finish() }
        binding.removeOrAddButton.setText(
            if (args.server != null) R.string.remove else R.string.storage_edit_smb_server_add
        )
        binding.removeOrAddButton.setOnClickListener {
            if (args.server != null) {
                remove()
            } else {
                saveOrAdd()
            }
        }

        if (savedInstanceState == null) {
            val server = args.server
            if (server != null) {
                val authority = server.authority
                binding.hostEdit.setText(authority.host)
                if (authority.port != Authority.DEFAULT_PORT) {
                    binding.portEdit.setText(authority.port)
                }
                when (val authentication = server.authentication) {
                    Authentication.GUEST ->
                        authenticationType = AuthenticationType.GUEST
                    Authentication.ANONYMOUS ->
                        authenticationType = AuthenticationType.ANONYMOUS
                    else -> {
                        authenticationType = AuthenticationType.PASSWORD
                        binding.usernameEdit.setText(authentication.username)
                        binding.passwordEdit.setText(authentication.password)
                        binding.domainEdit.setText(authentication.domain)
                    }
                }
            }
        }

        viewModel.connectStatefulLiveData.observe(viewLifecycleOwner) {
            onConnectStatefulChanged(it)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
        when (item.itemId) {
            android.R.id.home -> {
                // This recreates MainActivity but we cannot have singleTop as launch mode along
                // with document launch mode.
                //AppCompatActivity activity = (AppCompatActivity) requireActivity();
                //activity.onSupportNavigateUp();
                requireActivity().finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }

    private fun updateNamePlaceholder() {
        val host = binding.hostEdit.text.toString().takeIfNotEmpty()
        val port = binding.portEdit.text.toString().takeIfNotEmpty()
            .let { if (it != null) it.toIntOrNull() else Authority.DEFAULT_PORT }
        binding.nameLayout.placeholderText = if (host != null && port != null) {
            Authority(host, port).toString()
        } else if (host != null) {
            host
        } else {
            getString(R.string.storage_edit_smb_server_name_placeholder)
        }
    }

    private var authenticationType: AuthenticationType
        get() {
            val adapter = binding.authenticationTypeEdit.adapter
            val items = List(adapter.count) { adapter.getItem(it) as CharSequence }
            val selectedItem = binding.authenticationTypeEdit.text
            val selectedIndex = items.indexOfFirst { TextUtils.equals(it, selectedItem) }
            return AuthenticationType.values()[selectedIndex]
        }
        set(value) {
            val adapter = binding.authenticationTypeEdit.adapter
            val item = adapter.getItem(value.ordinal) as CharSequence
            binding.authenticationTypeEdit.setText(item, false)
        }

    private fun onAuthenticationTypeChanged(authenticationType: AuthenticationType) {
        val isPasswordAuthentication = authenticationType == AuthenticationType.PASSWORD
        binding.authenticationTypeLayout.isErrorEnabled = isPasswordAuthentication
        binding.passwordAuthenticationLayout.isVisible = isPasswordAuthentication
    }

    private fun saveOrAdd() {
        val server = getServerOrSetError() ?: return
        Storages.addOrReplace(server)
        finish()
    }

    private fun connectAndAdd() {
        if (!viewModel.connectStatefulLiveData.isReady) {
            return
        }
        val server = getServerOrSetError() ?: return
        viewModel.connectStatefulLiveData.connect(server)
    }

    private fun onConnectStatefulChanged(connectStateful: Stateful<SmbServer>) {
        val liveData = viewModel.connectStatefulLiveData
        when (connectStateful) {
            is Loading -> {}
            is Failure -> {
                connectStateful.throwable.printStackTrace()
                showToast(connectStateful.throwable.toString())
                liveData.reset()
            }
            is Success -> {
                Storages.addOrReplace(connectStateful.value)
                finish()
                return
            }
        }
        val isConnecting = !liveData.isReady
        binding.progress.fadeToVisibilityUnsafe(isConnecting)
        binding.scrollView.fadeToVisibilityUnsafe(!isConnecting)
        binding.saveOrConnectAndAddButton.isEnabled = !isConnecting
        binding.removeOrAddButton.isEnabled = !isConnecting
    }

    private fun remove() {
        Storages.remove(args.server!!)
        finish()
    }

    private fun getServerOrSetError(): SmbServer? {
        var errorEdit: TextInputEditText? = null
        val host = binding.hostEdit.text.toString().takeIfNotEmpty()
        if (host == null) {
            binding.hostLayout.error =
                getString(R.string.storage_edit_smb_server_host_empty_error)
            if (errorEdit == null) {
                errorEdit = binding.hostEdit
            }
        }
        val port = binding.portEdit.text.toString().takeIfNotEmpty()
            .let { if (it != null) it.toIntOrNull() else Authority.DEFAULT_PORT }
        if (port == null) {
            binding.portLayout.error = getString(R.string.storage_edit_smb_server_port_error)
            if (errorEdit == null) {
                errorEdit = binding.portEdit
            }
        }
        val name = binding.nameEdit.text.toString().takeIfNotEmpty()
        val authentication = when (authenticationType) {
            AuthenticationType.PASSWORD -> {
                val username = binding.usernameEdit.text.toString().takeIfNotEmpty()
                if (username == null) {
                    binding.usernameLayout.error =
                        getString(R.string.storage_edit_smb_server_username_empty_error)
                    if (errorEdit == null) {
                        errorEdit = binding.usernameEdit
                    }
                }
                val password = binding.passwordEdit.text.toString().takeIfNotEmpty()
                if (password == null) {
                    binding.passwordLayout.error =
                        getString(R.string.storage_edit_smb_server_password_empty_error)
                    if (errorEdit == null) {
                        errorEdit = binding.passwordEdit
                    }
                }
                val domain = binding.domainEdit.text.toString().takeIfNotEmpty()
                if (errorEdit == null) Authentication(username!!, password!!, domain) else null
            }
            AuthenticationType.GUEST -> Authentication.GUEST
            AuthenticationType.ANONYMOUS -> Authentication.ANONYMOUS
        }
        if (errorEdit != null) {
            errorEdit.requestFocus()
            return null
        }
        val authority = Authority(host!!, port!!)
        return SmbServer(args.server?.id, name ?: authority.toString(), authority, authentication!!)
    }

    private fun finish() {
        requireActivity().finish()
    }

    @Parcelize
    class Args(val server: SmbServer?) : ParcelableArgs

    private enum class AuthenticationType {
        PASSWORD,
        GUEST,
        ANONYMOUS
    }
}
