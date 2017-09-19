package net.spals.appbuilder.app.core.monitor;

import io.opentracing.BaseSpan;
import io.opentracing.contrib.jaxrs2.server.ServerSpanDecorator;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.core.MultivaluedMap;
import java.util.List;
import java.util.Map;

/**
 * Custom {@link ServerSpanDecorator} to add path parameter
 * values as tags on the {@link io.opentracing.Span}.
 *
 * @author tkral
 */
public class ParamSpanDecorator implements ServerSpanDecorator {

    @Override
    public void decorateRequest(final ContainerRequestContext requestContext,
                                final BaseSpan<?> span) {
        final MultivaluedMap<String, String> pathParameters = requestContext.getUriInfo().getPathParameters();
        for (Map.Entry<String, List<String>> entry: pathParameters.entrySet()) {
            span.setTag(String.format("param.%s", entry.getKey()),
                    String.valueOf(pathParameters.getFirst(entry.getKey())));
        }
    }

    @Override
    public void decorateResponse(final ContainerResponseContext responseContext,
                                 final BaseSpan<?> span) {  }
}
