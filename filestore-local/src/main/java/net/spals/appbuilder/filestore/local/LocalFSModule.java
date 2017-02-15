package net.spals.appbuilder.filestore.local;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.name.Names;
import com.typesafe.config.Config;
import net.spals.appbuilder.annotations.config.ServiceConfig;
import net.spals.appbuilder.annotations.service.AutoBindModule;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author tkral
 */
@AutoBindModule
class LocalFSModule extends AbstractModule {

    private final Path basePath;

    @Inject
    LocalFSModule(@ServiceConfig final Config serviceConfig) {
        this.basePath = Paths.get(serviceConfig.getString("local.fileStore.basePath"));
    }

    @Override
    protected void configure() {
        binder().bind(Path.class).annotatedWith(Names.named("fileStore")).toInstance(basePath);
    }
}
