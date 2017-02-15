package net.spals.appbuilder.filestore.core;

import net.spals.appbuilder.filestore.core.model.FileStoreKey;
import net.spals.appbuilder.filestore.core.model.FileStoreMetadata;
import net.spals.appbuilder.filestore.core.model.PutFileStoreRequest;

import java.io.InputStream;
import java.util.Optional;

/**
 * @author tkral
 */
public interface FileStore {

    boolean deleteFile(FileStoreKey key);

    Optional<FileStoreMetadata> getFileMetadata(FileStoreKey key);

    Optional<InputStream> getFile(FileStoreKey key);

    FileStoreMetadata putFile(FileStoreKey key, PutFileStoreRequest request);
}
