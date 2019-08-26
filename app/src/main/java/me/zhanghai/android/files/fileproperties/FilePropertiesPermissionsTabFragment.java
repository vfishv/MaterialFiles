/*
 * Copyright (c) 2018 Hai Zhang <dreaming.in.code.zh@gmail.com>
 * All Rights Reserved.
 */

package me.zhanghai.android.files.fileproperties;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import java.util.Set;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDialogFragment;
import butterknife.BindView;
import butterknife.ButterKnife;
import java8.nio.file.attribute.BasicFileAttributes;
import me.zhanghai.android.files.R;
import me.zhanghai.android.files.filelist.FileItem;
import me.zhanghai.android.files.provider.common.PosixFileAttributes;
import me.zhanghai.android.files.provider.common.PosixFileMode;
import me.zhanghai.android.files.provider.common.PosixFileModeBit;
import me.zhanghai.android.files.provider.common.PosixGroup;
import me.zhanghai.android.files.provider.common.PosixUser;
import me.zhanghai.android.files.util.FragmentUtils;
import me.zhanghai.android.files.util.ObjectUtils;
import me.zhanghai.android.files.util.ViewUtils;

public class FilePropertiesPermissionsTabFragment extends AppCompatDialogFragment {

    private static final String KEY_PREFIX = FilePropertiesPermissionsTabFragment.class.getName()
            + '.';

    private static final String EXTRA_FILE = KEY_PREFIX + "FILE";

    @BindView(R.id.owner)
    Button mOwnerButton;
    @BindView(R.id.group)
    Button mGroupButton;
    @BindView(R.id.mode)
    Button mModeButton;
    @BindView(R.id.selinux_context_layout)
    ViewGroup mSeLinuxContextLayout;
    @BindView(R.id.selinux_context)
    Button mSeLinuxContextButton;

    @NonNull
    private FileItem mExtraFile;

    /**
     * @deprecated Use {@link #newInstance(FileItem)} instead.
     */
    public FilePropertiesPermissionsTabFragment() {}

    public static boolean isAvailable(@NonNull FileItem file) {
        BasicFileAttributes attributes = file.getAttributes();
        if (attributes instanceof PosixFileAttributes) {
            PosixFileAttributes posixAttributes = (PosixFileAttributes) attributes;
            return posixAttributes.owner() != null || posixAttributes.group() != null
                    || posixAttributes.mode() != null;
        }
        return false;
    }

    @NonNull
    public static FilePropertiesPermissionsTabFragment newInstance(@NonNull FileItem file) {
        //noinspection deprecation
        FilePropertiesPermissionsTabFragment fragment = new FilePropertiesPermissionsTabFragment();
        FragmentUtils.getArgumentsBuilder(fragment)
                .putParcelable(EXTRA_FILE, file);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mExtraFile = getArguments().getParcelable(EXTRA_FILE);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.file_properties_permissions_tab_fragment, container,
                false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ButterKnife.bind(this, view);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        BasicFileAttributes attributes = mExtraFile.getAttributes();
        if (attributes instanceof PosixFileAttributes) {
            PosixFileAttributes posixAttributes = (PosixFileAttributes) attributes;
            PosixUser owner = posixAttributes.owner();
            String ownerString = owner != null ? owner.getName() != null ? getString(
                    R.string.file_properties_permissions_owner_format, owner.getName(),
                    owner.getId()) : String.valueOf(owner.getId()) : getString(R.string.unknown);
            mOwnerButton.setText(ownerString);
            PosixGroup group = posixAttributes.group();
            String groupString = group != null ? group.getName() != null ? getString(
                    R.string.file_properties_permissions_group_format, group.getName(),
                    group.getId()) : String.valueOf(group.getId()) : getString(R.string.unknown);
            mGroupButton.setText(groupString);
            Set<PosixFileModeBit> mode = posixAttributes.mode();
            String modeString = mode != null ? getString(
                    R.string.file_properties_permissions_mode_format, PosixFileMode.toString(mode),
                    PosixFileMode.toInt(mode)) : getString(R.string.unknown);
            mModeButton.setText(modeString);
            String seLinuxContext = ObjectUtils.toStringOrNull(posixAttributes.seLinuxContext());
            ViewUtils.setVisibleOrGone(mSeLinuxContextLayout, seLinuxContext != null);
            mSeLinuxContextButton.setText(seLinuxContext);
        }
        // TODO: Other attributes?
    }
}
