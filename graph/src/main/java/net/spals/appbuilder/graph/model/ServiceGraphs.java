package net.spals.appbuilder.graph.model;

import net.spals.appbuilder.config.service.ServiceScan;

import java.util.concurrent.atomic.AtomicReference;

/**
 * @author tkral
 */
public class ServiceGraphs {

    private final String applicationName;
    private final ServiceScan serviceScan;

    private final ServiceGraph serviceGraph;

    private final AtomicReference<ServiceDAG> serviceDAGRef = new AtomicReference<>(null);
    private final AtomicReference<ServiceTree> serviceTreeRef = new AtomicReference<>(null);

    public ServiceGraphs(final String applicationName,
                         final ServiceGraph serviceGraph,
                         final ServiceScan serviceScan) {
        this.applicationName = applicationName;
        this.serviceGraph = serviceGraph;
        this.serviceScan = serviceScan;
    }

    public ServiceGraph getServiceGraph() {
        return serviceGraph;
    }

    public ServiceDAG getServiceDAG() {
        serviceDAGRef.compareAndSet(null,
            new ServiceDAGConverter(applicationName, serviceScan).convertFrom(serviceGraph));
        return serviceDAGRef.get();
    }

    public ServiceTree getServiceTree() {
        serviceTreeRef.compareAndSet(null,
            new ServiceTreeConverter().convertFrom(getServiceDAG()));
        return serviceTreeRef.get();
    }
}
