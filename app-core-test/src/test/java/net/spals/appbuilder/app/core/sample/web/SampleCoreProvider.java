package net.spals.appbuilder.app.core.sample.web;

import net.spals.appbuilder.annotations.service.AutoBindSingleton;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

/**
 * A sample web provider.
 *
 * @author tkral
 */
@AutoBindSingleton
@Provider
public class SampleCoreProvider {

    SampleCoreProvider() { }
}
