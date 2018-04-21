package net.spals.appbuilder.app.grpc.sample;

import net.spals.appbuilder.annotations.service.AutoBindInSet;

/**
 * An auto-bound multi service for {@link SampleGrpcWebApp}
 *
 * @author tkral
 */
@AutoBindInSet(baseClass = SampleGrpcCustomSet.class)
class SampleGrpcCustomSetInstance implements SampleGrpcCustomSet {
}
