package net.spals.appbuilder.filestore.core.localfs;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import net.spals.appbuilder.annotations.service.AutoBindInMap;
import net.spals.appbuilder.filestore.core.FileStorePlugin;
import net.spals.appbuilder.filestore.core.model.FileMetadata;
import net.spals.appbuilder.filestore.core.model.FileSecurityLevel;
import net.spals.appbuilder.filestore.core.model.FileStoreKey;
import net.spals.appbuilder.filestore.core.model.PutFileRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Optional;

import static java.nio.file.attribute.PosixFilePermission.*;
import static net.spals.appbuilder.filestore.core.model.FileStoreLocation.LOCAL;

/**
 * A {@link FileStorePlugin} implementation which
 * uses the local filesystem.
 *
 * @author tkral
 */
@AutoBindInMap(baseClass = FileStorePlugin.class, key = "localFS")
class LocalFSFileStorePlugin implements FileStorePlugin {
    private static final Logger LOGGER = LoggerFactory.getLogger(LocalFSFileStorePlugin.class);

    private final Path basePath;

    @Inject
    LocalFSFileStorePlugin(@Named("localFS.fileStore") final Path basePath) {
        this.basePath = basePath;
    }

    @Override
    public boolean deleteFile(final FileStoreKey key) throws IOException {
        final Path filePath = resolveFilePath(key);
        try {
            Files.deleteIfExists(filePath);
            return true;
        } catch (IOException e) {
            LOGGER.error(String.format("Unexpected error while deleting file from local filesystem: %s", filePath), e);
            throw e;
        }
    }

    @Override
    public Optional<InputStream> getFileContent(final FileStoreKey key) throws IOException {
        final Path filePath = resolveFilePath(key);
        if (!Files.exists(filePath)) {
            return Optional.empty();
        }

        try {
            return Optional.of(filePath.toUri().toURL().openStream());
        } catch (IOException e) {
            LOGGER.error(String.format("Unexpected error while getting file contents from local filesystem: %s", filePath), e);
            throw e;
        }
    }

    @Override
    public Optional<FileMetadata> getFileMetadata(final FileStoreKey key) throws IOException {
        final Path filePath = resolveFilePath(key);
        if (!Files.exists(filePath)) {
            return Optional.empty();
        }

        try {
            final FileSecurityLevel fileSecurity = Files.getPosixFilePermissions(filePath).contains(OTHERS_READ) ?
                    FileSecurityLevel.PUBLIC : FileSecurityLevel.PRIVATE;
            return Optional.of(new FileMetadata.Builder().setSecurityLevel(fileSecurity).setStoreLocation(LOCAL)
                    .setURI(filePath.toUri()).build());
        } catch (IOException e) {
            LOGGER.error(String.format("Unexpected error while getting file metadata from local filesystem: %s", filePath), e);
            throw e;
        }
    }

    @Override
    public FileMetadata putFile(final FileStoreKey key, final PutFileRequest request) throws IOException {
        final Path filePath = resolveFilePath(key);
        try {
            Files.createDirectories(filePath.getParent());
            if (!Files.exists(filePath)) {
                Files.createFile(filePath);
            }

            // Copy the file stream into the file
            java.nio.file.Files.copy(request.getFileStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            // Add security settings to file
            final ImmutableSet.Builder<PosixFilePermission> filePermsBuilder = ImmutableSet.<PosixFilePermission>builder()
                    .add(OWNER_READ, OWNER_EXECUTE, GROUP_READ, GROUP_EXECUTE);
            if (request.getFileSecurityLevel().isPublic()) {
                filePermsBuilder.add(OTHERS_READ);
            }

            Files.setPosixFilePermissions(filePath, filePermsBuilder.build());
            return new FileMetadata.Builder().setSecurityLevel(request.getFileSecurityLevel()).setStoreLocation(LOCAL)
                    .setURI(filePath.toUri()).build();
        } catch (IOException e) {
            LOGGER.error(String.format("Unexpected error while putting file in local filesystem: %s", filePath), e);
            throw e;
        }
    }

    @VisibleForTesting
    Path resolveFilePath(final FileStoreKey key) {
        return basePath.resolve(key.toGlobalId("/"));
    }
}
