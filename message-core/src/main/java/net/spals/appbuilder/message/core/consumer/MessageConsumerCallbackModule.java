package net.spals.appbuilder.message.core.consumer;

import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.google.inject.multibindings.MapBinder;
import net.spals.appbuilder.annotations.service.AutoBindModule;

/**
 * A Guice {@link Module} which initializes the injected map
 * of {@link MessageConsumerCallback}s.
 *
 * We do this for bootstrapping reasons. If the app author
 * has scanned the message consumer services, but doesn't
 * have any {@link MessageConsumerCallback}s defined, we
 * still want the system to boot.
 *
 * @author tkral
 */
@AutoBindModule
class MessageConsumerCallbackModule extends AbstractModule {

    @Override
    protected void configure() {
        MapBinder.newMapBinder(binder(), String.class, MessageConsumerCallback.class);
    }
}

