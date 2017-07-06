package net.spals.appbuilder.app.core.sample.web;

import net.spals.appbuilder.annotations.service.AutoBindSingleton;

import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

/**
 * A sample web dynamic feature.
 *
 * @author tkral
 */
@AutoBindSingleton
public class SampleCoreDynamicFeature implements DynamicFeature {

    SampleCoreDynamicFeature() { }

    @Override
    public void configure(final ResourceInfo resourceInfo, final FeatureContext context) { }
}
