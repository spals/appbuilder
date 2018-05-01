package net.spals.appbuilder.app.examples.dropwizard.cors;

import io.dropwizard.Application;
import io.dropwizard.Configuration;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import net.spals.appbuilder.app.dropwizard.DropwizardWebApp;
import net.spals.appbuilder.config.service.ServiceScan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link DropwizardWebApp} which tests CORS registration.
 *
 * @author tkral
 */
public class CorsDropwizardWebApp extends Application<Configuration> {
    private static final Logger LOGGER = LoggerFactory.getLogger(CorsDropwizardWebApp.class);

    public static void main(final String[] args) throws Throwable {
        new CorsDropwizardWebApp().run("server");
    }

    private DropwizardWebApp.Builder webAppDelegateBuilder;

    @Override
    public void initialize(final Bootstrap<Configuration> bootstrap) {
        webAppDelegateBuilder = new DropwizardWebApp.Builder(bootstrap, LOGGER)
            .enableCors()
            .setServiceScan(new ServiceScan.Builder()
                .addServicePackages("net.spals.appbuilder.app.examples.dropwizard.cors")
                .build());
    }

    @Override
    public void run(final Configuration configuration, final Environment env) {
        webAppDelegateBuilder.setEnvironment(env).build();
    }
}
