package org.openbase.bco.api.graphql.discovery;

/*-
 * #%L
 * BCO GraphQL API
 * %%
 * Copyright (C) 2020 openbase.org
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

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
