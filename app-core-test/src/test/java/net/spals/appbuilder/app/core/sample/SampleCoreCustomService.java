package net.spals.appbuilder.app.core.sample;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import net.spals.appbuilder.annotations.service.AutoBindSingleton;

/**
 * A sample auto-bound service.
 *
 * @author tkral
 */
@AutoBindSingleton
public class SampleCoreCustomService {

    @Inject
    SampleCoreCustomService(@Named("AutoBoundModule") final String autoBoundModuleName) {  }
}
