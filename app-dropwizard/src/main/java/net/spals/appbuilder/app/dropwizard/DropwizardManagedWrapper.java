package net.spals.appbuilder.app.dropwizard;

import io.dropwizard.lifecycle.Managed;
import net.spals.appbuilder.executor.core.ManagedExecutorServiceRegistry;
import net.spals.appbuilder.message.core.consumer.MessageConsumer;

/**
 * Convenience methods for wrapping services
 * in Dropwizard's {@link Managed} lifecycle
 * framework.
 *
 * @author tkral
 */
public class DropwizardManagedWrapper {

    private DropwizardManagedWrapper() {  }

    public static Managed wrap(final ManagedExecutorServiceRegistry executorServiceRegistry) {
        return new Managed() {
            @Override
            public void start() throws Exception {
                executorServiceRegistry.start();
            }

            @Override
            public void stop() throws Exception {
                executorServiceRegistry.stop();
            }
        };
    }

    public static Managed wrap(final MessageConsumer messageConsumer) {
        return new Managed() {
            @Override
            public void start() throws Exception {
                messageConsumer.start();
            }

            @Override
            public void stop() throws Exception {
                messageConsumer.stop();
            }
        };
    }
}
