package net.spals.appbuilder.app.dropwizard.sample;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import net.spals.appbuilder.annotations.service.AutoBindSingleton;

/**
 * An auto-bound service for {@link SampleDropwizardWebApp}
 *
 * @author tkral
 */
@AutoBindSingleton
public class SampleDropwizardCustomService {

    @Inject
    SampleDropwizardCustomService(@Named("AutoBoundModule") final String autoBoundModuleName) {  }
}
