package net.spals.appbuilder.store.local;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Provider;
import com.netflix.governator.annotations.Configuration;
import net.spals.appbuilder.annotations.service.AutoBindProvider;
import org.mapdb.DB;
import org.mapdb.DBMaker;

import java.util.Optional;

/**
 * @author tkral
 */
@AutoBindProvider
class MapDBProvider implements Provider<DB> {

    @Configuration("local.store.file")
    @VisibleForTesting
    private volatile String storeFilePath;

    @Override
    public DB get() {
        return Optional.ofNullable(storeFilePath).map(filePath -> DBMaker.fileDB(filePath).closeOnJvmShutdown().make())
                .orElseGet(() -> DBMaker.memoryDB().closeOnJvmShutdown().make());
    }
}
