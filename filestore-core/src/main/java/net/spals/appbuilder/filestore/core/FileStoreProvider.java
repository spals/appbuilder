package net.spals.appbuilder.filestore.core;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.netflix.governator.annotations.Configuration;
import com.typesafe.config.ConfigException;
import net.spals.appbuilder.annotations.service.AutoBindProvider;
import net.spals.appbuilder.filestore.core.model.FileMetadata;
import net.spals.appbuilder.filestore.core.model.FileStoreKey;
import net.spals.appbuilder.filestore.core.model.PutFileRequest;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Optional;

/**
 * @author tkral
 */
@AutoBindProvider
class FileStoreProvider implements Provider<FileStore> {

    @Configuration("fileStore.system")
    private volatile String storeSystem;

    private final Map<String, FileStorePlugin> storePluginMap;

    @Inject
    FileStoreProvider(final Map<String, FileStorePlugin> storePluginMap) {
        this.storePluginMap = storePluginMap;
    }

    @Override
    public FileStore get() {
        final FileStorePlugin storePlugin = Optional.ofNullable(storePluginMap.get(storeSystem))
                .orElseThrow(() -> new ConfigException.BadValue("fileStore.system",
                        "No File Store plugin found for : " + storeSystem));

        return new DelegatingFileStore(storePlugin);
    }

    @VisibleForTesting
    static class DelegatingFileStore implements FileStore {

        private final FileStorePlugin pluginDelegate;

        DelegatingFileStore(final FileStorePlugin pluginDelegate) {
            this.pluginDelegate = pluginDelegate;
        }

        @Override
        public boolean deleteFile(final FileStoreKey key) throws IOException {
            return pluginDelegate.deleteFile(key);
        }

        @Override
        public Optional<InputStream> getFileContent(final FileStoreKey key) throws IOException {
            return pluginDelegate.getFileContent(key);
        }

        @Override
        public Optional<FileMetadata> getFileMetadata(final FileStoreKey key) throws IOException {
            return pluginDelegate.getFileMetadata(key);
        }

        @Override
        public FileMetadata putFile(final FileStoreKey key, final PutFileRequest request) throws IOException {
            return pluginDelegate.putFile(key, request);
        }
    }
}
