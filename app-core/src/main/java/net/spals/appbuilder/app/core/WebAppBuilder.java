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
 * {@link SimpleAppBuilder}.
 *
 * @author tkral
 */
public interface WebAppBuilder<A extends App> extends SimpleAppBuilder<A> {

    WebAppBuilder<A> disableWebServerAutoBinding();

    WebAppBuilder<A> enableRequestScoping();
}
