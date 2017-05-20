package net.spals.appbuilder.app.dropwizard.minimal;

import com.google.common.annotations.VisibleForTesting;
import io.dropwizard.Application;
import io.dropwizard.Configuration;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import net.spals.appbuilder.app.dropwizard.DropwizardWebApp;
import net.spals.appbuilder.app.dropwizard.sample.SampleDropwizardWebApp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A minimally viable [[DropwizardWebApp]]
 *
 * @author tkral
 */
public class MinimalDropwizardWebApp extends Application<Configuration> {
    private static final Logger LOGGER = LoggerFactory.getLogger(MinimalDropwizardWebApp.class);

    public static void main(final String[] args) throws Throwable {
        new MinimalDropwizardWebApp().run("server");
    }

    private DropwizardWebApp.Builder webAppDelegateBuilder;
    private DropwizardWebApp webAppDelegate;

    @VisibleForTesting
    public DropwizardWebApp getDelegate() {
        return webAppDelegate;
    }

    @Override
    public void initialize(final Bootstrap<Configuration> bootstrap) {
        this.webAppDelegateBuilder = new DropwizardWebApp.Builder(bootstrap, LOGGER);
    }

    @Override
    public void run(Configuration configuration, Environment env) throws Exception {
        this.webAppDelegate = webAppDelegateBuilder.setEnvironment(env).build();
    }
}
