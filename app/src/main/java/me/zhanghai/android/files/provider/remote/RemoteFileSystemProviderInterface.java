/*
 * Copyright (c) 2019 Hai Zhang <dreaming.in.code.zh@gmail.com>
 * All Rights Reserved.
 */

package me.zhanghai.android.files.provider.remote;

import java.io.IOException;

import androidx.annotation.NonNull;
import java8.nio.file.AccessMode;
import java8.nio.file.DirectoryStream;
import java8.nio.file.FileStore;
import java8.nio.file.LinkOption;
import java8.nio.file.Path;
import java8.nio.file.attribute.BasicFileAttributes;
import java8.nio.file.attribute.FileAttribute;
import java8.nio.file.spi.FileSystemProvider;

public class RemoteFileSystemProviderInterface extends IRemoteFileSystemProvider.Stub {

    @NonNull
    private final FileSystemProvider mProvider;

    public RemoteFileSystemProviderInterface(@NonNull FileSystemProvider provider) {
        mProvider = provider;
    }

    @Override
    public ParcelableDirectoryStream newDirectoryStream(
            @NonNull ParcelableObject parcelableDirectory,
            @NonNull ParcelableObject parcelableFilter,
            @NonNull ParcelableIoException ioException) {
        Path directory = parcelableDirectory.get();
        DirectoryStream.Filter<? super Path> filter = parcelableFilter.get();
        ParcelableDirectoryStream parcelableDirectoryStream;
        try (DirectoryStream<Path> directoryStream = mProvider.newDirectoryStream(directory,
                filter)) {
            parcelableDirectoryStream = new ParcelableDirectoryStream(directoryStream);
        } catch (IOException e) {
            ioException.set(e);
            return null;
        }
        return parcelableDirectoryStream;
    }

    @Override
    public void createDirectory(@NonNull ParcelableObject parcelableDirectory,
                                @NonNull ParcelableFileAttributes parcelableAttributes,
                                @NonNull ParcelableIoException ioException) {
        Path directory = parcelableDirectory.get();
        FileAttribute<?>[] attributes = parcelableAttributes.get();
        try {
            mProvider.createDirectory(directory, attributes);
        } catch (IOException e) {
            ioException.set(e);
        }
    }

    @Override
    public void createSymbolicLink(@NonNull ParcelableObject parcelableLink,
                                   @NonNull ParcelableObject parcelableTarget,
                                   @NonNull ParcelableFileAttributes parcelableAttributes,
                                   @NonNull ParcelableIoException ioException) {
        Path link = parcelableLink.get();
        Path target = parcelableTarget.get();
        FileAttribute<?>[] attributes = parcelableAttributes.get();
        try {
            mProvider.createSymbolicLink(link, target, attributes);
        } catch (IOException e) {
            ioException.set(e);
        }
    }

    @Override
    public void createLink(@NonNull ParcelableObject parcelableLink,
                           @NonNull ParcelableObject parcelableExisting,
                           @NonNull ParcelableIoException ioException) {
        Path link = parcelableLink.get();
        Path existing = parcelableExisting.get();
        try {
            mProvider.createLink(link, existing);
        } catch (IOException e) {
            ioException.set(e);
        }
    }

    @Override
    public void delete(@NonNull ParcelableObject parcelablePath,
                       @NonNull ParcelableIoException ioException) {
        Path path = parcelablePath.get();
        try {
            mProvider.delete(path);
        } catch (IOException e) {
            ioException.set(e);
        }
    }

    @Override
    public ParcelableObject readSymbolicLink(@NonNull ParcelableObject parcelableLink,
                                             @NonNull ParcelableIoException ioException) {
        Path link = parcelableLink.get();
        Path target;
        try {
            target = mProvider.readSymbolicLink(link);
        } catch (IOException e) {
            ioException.set(e);
            return null;
        }
        return new ParcelableObject(target);
    }

    @Override
    public boolean isSameFile(@NonNull ParcelableObject parcelablePath,
                              @NonNull ParcelableObject parcelablePath2,
                              @NonNull ParcelableIoException ioException) {
        Path path = parcelablePath.get();
        Path path2 = parcelablePath2.get();
        try {
            return mProvider.isSameFile(path, path2);
        } catch (IOException e) {
            ioException.set(e);
            return false;
        }
    }

    @Override
    public boolean isHidden(@NonNull ParcelableObject parcelablePath,
                            @NonNull ParcelableIoException ioException) {
        Path path = parcelablePath.get();
        try {
            return mProvider.isHidden(path);
        } catch (IOException e) {
            ioException.set(e);
            return false;
        }
    }

    @NonNull
    @Override
    public ParcelableObject getFileStore(@NonNull ParcelableObject parcelablePath,
                                         @NonNull ParcelableIoException ioException) {
        Path path = parcelablePath.get();
        FileStore fileStore;
        try {
            fileStore = mProvider.getFileStore(path);
        } catch (IOException e) {
            ioException.set(e);
            return null;
        }
        return new ParcelableObject(fileStore);
    }

    @Override
    public void checkAccess(@NonNull ParcelableObject parcelablePath,
                            @NonNull ParcelableSerializable parcelableModes,
                            @NonNull ParcelableIoException ioException) {
        Path path = parcelablePath.get();
        AccessMode[] modes = parcelableModes.get();
        try {
            mProvider.checkAccess(path, modes);
        } catch (IOException e) {
            ioException.set(e);
        }
    }

    @NonNull
    @Override
    public ParcelableObject readAttributes(@NonNull ParcelableObject parcelablePath,
                                           @NonNull ParcelableSerializable parcelableType,
                                           @NonNull ParcelableSerializable parcelableOptions,
                                           @NonNull ParcelableIoException ioException) {
        Path path = parcelablePath.get();
        Class<? extends BasicFileAttributes> type = parcelableType.get();
        LinkOption[] options = parcelableOptions.get();
        BasicFileAttributes attributes;
        try {
            attributes = mProvider.readAttributes(path, type, options);
        } catch (IOException e) {
            ioException.set(e);
            return null;
        }
        return new ParcelableObject(attributes);
    }
}
