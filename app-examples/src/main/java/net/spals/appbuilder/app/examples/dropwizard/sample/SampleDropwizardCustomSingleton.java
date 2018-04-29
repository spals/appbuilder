package net.spals.appbuilder.app.examples.dropwizard.sample;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import net.spals.appbuilder.annotations.service.AutoBindSingleton;

/**
 * An auto-bound singleton service for {@link SampleDropwizardWebApp}
 *
 * @author tkral
 */
@AutoBindSingleton
public class SampleDropwizardCustomSingleton {

    @Inject
    SampleDropwizardCustomSingleton(@Named("AutoBoundModule") final String autoBoundModuleName) {  }
}
