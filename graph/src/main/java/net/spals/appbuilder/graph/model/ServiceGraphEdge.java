package net.spals.appbuilder.graph.model;

import org.jgrapht.graph.DefaultEdge;

/**
 * @author tkral
 */
public class ServiceGraphEdge extends DefaultEdge {

    public ServiceGraphVertex<?> getSourceVertex() {
        return (ServiceGraphVertex<?>)getSource();
    }

    public ServiceGraphVertex<?> getTargetVertex() {
        return (ServiceGraphVertex<?>)getTarget();
    }
}
