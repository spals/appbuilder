package net.spals.appbuilder.filestore.core;

import net.spals.appbuilder.filestore.core.model.FileMetadata;
import net.spals.appbuilder.filestore.core.model.FileStoreKey;
import net.spals.appbuilder.filestore.core.model.PutFileRequest;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

/**
 * @author tkral
 */
public interface FileStore {

    boolean deleteFile(FileStoreKey key) throws IOException;

    Optional<InputStream> getFileContent(FileStoreKey key) throws IOException;

    Optional<FileMetadata> getFileMetadata(FileStoreKey key) throws IOException;

    FileMetadata putFile(FileStoreKey key, PutFileRequest request) throws IOException;
}
