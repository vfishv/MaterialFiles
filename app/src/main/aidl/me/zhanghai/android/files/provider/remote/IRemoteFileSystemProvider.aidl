package me.zhanghai.android.files.provider.remote;

import me.zhanghai.android.files.provider.remote.ParcelableDirectoryStream;
import me.zhanghai.android.files.provider.remote.ParcelableFileAttributes;
import me.zhanghai.android.files.provider.remote.ParcelableIoException;
import me.zhanghai.android.files.provider.remote.ParcelableObject;
import me.zhanghai.android.files.provider.remote.ParcelableSerializable;

interface IRemoteFileSystemProvider {

    ParcelableDirectoryStream newDirectoryStream(in ParcelableObject parcelableDirectory,
            in ParcelableObject parcelableFilter, out ParcelableIoException ioException);

    void createDirectory(in ParcelableObject parcelableDirectory,
            in ParcelableFileAttributes parcelableAttributes,
            out ParcelableIoException ioException);

    void createSymbolicLink(in ParcelableObject parcelableLink,
            in ParcelableObject parcelableTarget, in ParcelableFileAttributes parcelableAttributes,
            out ParcelableIoException ioException);

    void createLink(in ParcelableObject parcelableLink, in ParcelableObject parcelableExisting,
            out ParcelableIoException ioException);

    void delete(in ParcelableObject parcelablePath, out ParcelableIoException ioException);

    ParcelableObject readSymbolicLink(in ParcelableObject parcelableLink,
            out ParcelableIoException ioException);

    boolean isSameFile(in ParcelableObject parcelablePath, in ParcelableObject parcelablePath2,
            out ParcelableIoException ioException);

    boolean isHidden(in ParcelableObject parcelablePath, out ParcelableIoException ioException);

    ParcelableObject getFileStore(in ParcelableObject parcelablePath,
            out ParcelableIoException ioException);

    void checkAccess(in ParcelableObject parcelablePath, in ParcelableSerializable parcelableModes,
            out ParcelableIoException ioException);

    ParcelableObject readAttributes(in ParcelableObject parcelablePath,
            in ParcelableSerializable parcelableType, in ParcelableSerializable parcelableOptions,
            out ParcelableIoException ioException);
}
