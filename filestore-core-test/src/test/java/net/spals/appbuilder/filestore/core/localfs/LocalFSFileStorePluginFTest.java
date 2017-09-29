package net.spals.appbuilder.filestore.core.localfs;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.google.common.io.Resources;
import net.spals.appbuilder.filestore.core.model.*;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import static java.nio.file.attribute.PosixFilePermission.OTHERS_READ;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Functional tests for {@link LocalFSFileStorePlugin}.
 *
 * @author tkral
 */
public class LocalFSFileStorePluginFTest {

    @Test
    public void testPutFile() throws IOException {
        final Path basePath = Files.createTempDir().toPath();
        final LocalFSFileStorePlugin fileStorePlugin = new LocalFSFileStorePlugin(basePath);

        final FileStoreKey fileKey = new FileStoreKey.Builder()
                .setPartition("LocalFSFileStorePluginFTest")
                .setId("testPutFile.txt")
                .build();
        final InputStream fileStream = new ByteArrayInputStream("abcde".getBytes());
        final PutFileRequest putFileRequest = new PutFileRequest.Builder().setFileStream(fileStream)
                .setFileSecurityLevel(FileSecurityLevel.PUBLIC).build();
        final FileMetadata fileMetadata = fileStorePlugin.putFile(fileKey, putFileRequest);
        assertThat(fileMetadata.getURI().getScheme(), is("file"));
        assertThat(fileMetadata.getURI().getPath(),
                is(String.format("%s/LocalFSFileStorePluginFTest/testPutFile.txt", basePath)));
        assertThat(fileMetadata.getSecurityLevel(), is(FileSecurityLevel.PUBLIC));
        assertThat(fileMetadata.getSecurityLevel().isPublic(), is(true));
        assertThat(fileMetadata.getStoreLocation(), is(FileStoreLocation.LOCAL));
        assertThat(fileMetadata.getStoreLocation().isLocal(), is(true));
        assertThat(java.nio.file.Files.getPosixFilePermissions(Paths.get(fileMetadata.getURI())), hasItem(OTHERS_READ));
    }

    @Test
    public void testPutFileReplace() throws IOException {
        final Path basePath = Files.createTempDir().toPath();
        final LocalFSFileStorePlugin fileStorePlugin = new LocalFSFileStorePlugin(basePath);

        final FileStoreKey fileKey = new FileStoreKey.Builder()
                .setPartition("LocalFSFileStorePluginFTest")
                .setId("testPutFileReplace.txt")
                .build();
        final InputStream fileStream1 = new ByteArrayInputStream("abcde".getBytes());
        final PutFileRequest putFileRequest1 = new PutFileRequest.Builder().setFileStream(fileStream1)
                .setFileSecurityLevel(FileSecurityLevel.PUBLIC).build();
        fileStorePlugin.putFile(fileKey, putFileRequest1);
        final InputStream fileStream2 = new ByteArrayInputStream("12345".getBytes());
        final PutFileRequest putFileRequest2 = new PutFileRequest.Builder().setFileStream(fileStream2)
                .setFileSecurityLevel(FileSecurityLevel.PUBLIC).build();
        final FileMetadata fileMetadata = fileStorePlugin.putFile(fileKey, putFileRequest2);

        // Verify that the contents of the file have been replaced
        assertThat(Resources.asCharSource(fileMetadata.getURI().toURL(), Charsets.UTF_8).read(), is("12345"));
    }

    @Test
    public void testGetFile() throws IOException {
        final Path basePath = Files.createTempDir().toPath();
        final LocalFSFileStorePlugin fileStorePlugin = new LocalFSFileStorePlugin(basePath);

        final FileStoreKey fileKey = new FileStoreKey.Builder()
                .setPartition("LocalFSFileStorePluginFTest")
                .setId("testGetFile.txt")
                .build();
        final InputStream fileStream = new ByteArrayInputStream("abcde".getBytes());
        final PutFileRequest putFileRequest = new PutFileRequest.Builder().setFileStream(fileStream)
                .setFileSecurityLevel(FileSecurityLevel.PUBLIC).build();
        final FileMetadata putFileMetadata = fileStorePlugin.putFile(fileKey, putFileRequest);

        final Optional<FileMetadata> getFileMetadata = fileStorePlugin.getFileMetadata(fileKey);
        assertThat(getFileMetadata, not(Optional.empty()));
        assertThat(getFileMetadata.get(), is(putFileMetadata));
    }

    @Test
    public void testDeleteFile() throws IOException {
        final Path basePath = Files.createTempDir().toPath();
        final LocalFSFileStorePlugin fileStorePlugin = new LocalFSFileStorePlugin(basePath);

        final FileStoreKey fileKey = new FileStoreKey.Builder()
                .setPartition("LocalFSFileStorePluginFTest")
                .setId("testDeleteFile.txt")
                .build();
        final InputStream fileStream = new ByteArrayInputStream("abcde".getBytes());
        final PutFileRequest putFileRequest = new PutFileRequest.Builder().setFileStream(fileStream)
                .setFileSecurityLevel(FileSecurityLevel.PUBLIC).build();
        fileStorePlugin.putFile(fileKey, putFileRequest);
        final boolean deletedFile = fileStorePlugin.deleteFile(fileKey);
        final Optional<FileMetadata> getFileMetadata = fileStorePlugin.getFileMetadata(fileKey);

        assertThat(deletedFile, is(true));
        assertThat(getFileMetadata, is(Optional.empty()));
    }

    @Test
    public void testPrivateFile() throws IOException {
        final Path basePath = Files.createTempDir().toPath();
        final LocalFSFileStorePlugin fileStorePlugin = new LocalFSFileStorePlugin(basePath);

        final FileStoreKey fileKey = new FileStoreKey.Builder()
                .setPartition("LocalFSFileStorePluginFTest")
                .setId("testPrivateFile.txt")
                .build();
        final InputStream fileStream = new ByteArrayInputStream("abcde".getBytes());
        final PutFileRequest putFileRequest = new PutFileRequest.Builder().setFileStream(fileStream)
                .setFileSecurityLevel(FileSecurityLevel.PRIVATE).build();
        final FileMetadata fileMetadata = fileStorePlugin.putFile(fileKey, putFileRequest);

        assertThat(java.nio.file.Files.getPosixFilePermissions(Paths.get(fileMetadata.getURI())), not(hasItem(OTHERS_READ)));
        assertThat(fileStorePlugin.getFileMetadata(fileKey).get().getSecurityLevel(), is(FileSecurityLevel.PRIVATE));
        assertThat(fileStorePlugin.getFileMetadata(fileKey).get().getSecurityLevel().isPrivate(), is(true));
    }
}
