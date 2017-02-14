package net.spals.appbuilder.message.local;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Module;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;
import com.netflix.governator.guice.annotations.GovernatorConfiguration;
import com.typesafe.config.Config;
import net.spals.appbuilder.annotations.config.ServiceConfig;
import net.spals.appbuilder.annotations.service.AutoBindModule;
import net.spals.appbuilder.annotations.service.AutoBindSingleton;
import net.spals.appbuilder.message.core.model.Message;

import javax.annotation.PostConstruct;
import java.util.AbstractMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

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
        this.queueSize = Optional.of(serviceConfig).filter(config -> config.hasPath("local.queue.size"))
                .map(config -> config.getInt("local.queue.size")).orElse(Integer.MAX_VALUE);
    }

    @Override
    protected void configure() {
        binder().bind(new TypeLiteral<BlockingQueue<Message>>(){})
                .annotatedWith(Names.named("localMessageQueue"))
                .toInstance(new LinkedBlockingQueue<>(queueSize));
    }
}
