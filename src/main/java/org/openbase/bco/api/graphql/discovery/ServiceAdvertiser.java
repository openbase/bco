package org.openbase.bco.api.graphql.discovery;

import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.iface.Shutdownable;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;
import java.io.IOException;
import java.net.InetAddress;

public class ServiceAdvertiser implements Shutdownable {

    private static ServiceAdvertiser instance;

    private final JmDNS domainNameService;

    private ServiceAdvertiser() throws InstantiationException {
        try {
            this.domainNameService = JmDNS.create(InetAddress.getLocalHost());
        } catch (IOException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    public static synchronized ServiceAdvertiser getInstance() throws InstantiationException {
        if(instance == null) {
            instance = new ServiceAdvertiser();
        }
        return instance;
    }

    public ServiceInfo register(final String serviceName, final int port, final String path) throws CouldNotPerformException {
        try {
            // Register the service
            final ServiceInfo serviceInfo = ServiceInfo.create("_http._tcp.local.", serviceName, port, "path=" + path);
            domainNameService.registerService(serviceInfo);
            return serviceInfo;
        } catch (IOException ex) {
            throw new CouldNotPerformException("Could not register service!", ex);
        }
    }

    public void deregisterService(final ServiceInfo serviceInfo) {
        domainNameService.unregisterService(serviceInfo);
    }

    @Override
    public void shutdown() {
        domainNameService.unregisterAllServices();
    }
}
