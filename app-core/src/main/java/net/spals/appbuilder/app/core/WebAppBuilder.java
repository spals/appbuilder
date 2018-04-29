package net.spals.appbuilder.app.core;

/**
 * Contract definition for a builder which produces
 * an instance of a web {@link App}.
 *
 * A web app is defined as an application which
 * requires a web server (presumably to handle
 * API requests and responses).
 *
 * For applications which do not require a web server, see
 * {@link WorkerAppBuilder}.
 *
 * @author tkral
 */
public interface WebAppBuilder<A extends App> extends WorkerAppBuilder<A> {

    /**
     * Disable automatic auto-binding of web server
     * components.
     *
     * By default, web application will attempt to
     * automatically bind perceived web components.
     *
     * A consumer may disable this functionality
     * with this method.
     */
    WebAppBuilder<A> disableWebServerAutoBinding();

    /**
     * Allow a web server to use Cross-Origin Resource
     * Sharing.
     *
     * By default, this is disabled as a security measure.
     * But web applications may turn it on to support
     * specific use cases.
     *
     * Note that this will enable CORS for *all* API
     * endpoints.
     */
    WebAppBuilder<A> enableCors();

    /**
     * Allow bound services to be scoped per web
     * server request.
     *
     * This is off by default because request scoping
     * typically involves an added filter which may
     * have performance ramifications.
     */
    WebAppBuilder<A> enableRequestScoping();
}
