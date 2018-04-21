package net.spals.appbuilder.app.grpc.sample;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import net.spals.appbuilder.annotations.service.AutoBindSingleton;

/**
 * An auto-bound singleton service for {@link SampleGrpcWebApp}
 *
 * @author tkral
 */
@AutoBindSingleton
public class SampleGrpcCustomSingleton {

    @Inject
    SampleGrpcCustomSingleton(@Named("AutoBoundModule") final String autoBoundModuleName) {  }
}
