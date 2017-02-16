package net.spals.appbuilder.message.core.blockingqueue;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Module;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;
import com.typesafe.config.Config;
import net.spals.appbuilder.annotations.config.ServiceConfig;
import net.spals.appbuilder.annotations.service.AutoBindModule;

import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * A Guice {@link Module} which binds a {@link BlockingQueue}
 * to be used for local message production and consumption.
 *
 * @author tkral
 */
@AutoBindModule
class BlockingQueueModule extends AbstractModule {

    // local.queue.size
    private final Integer queueSize;

    @Inject
    BlockingQueueModule(@ServiceConfig final Config serviceConfig) {
        this.queueSize = Optional.of(serviceConfig).filter(config -> config.hasPath("blockingQueue.size"))
                .map(config -> config.getInt("blockingQueue.size")).orElse(Integer.MAX_VALUE);
    }

    @Override
    protected void configure() {
        binder().bind(new TypeLiteral<BlockingQueue<byte[]>>(){})
                .annotatedWith(Names.named("blockingMessageQueue"))
                .toInstance(new LinkedBlockingQueue<>(queueSize));
    }
}
