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

import org.openbase.jps.core.JPService;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.iface.Shutdownable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceInfo.Fields;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.*;

public class ServiceAdvertiser implements Shutdownable {

    private static Logger LOGGER = LoggerFactory.getLogger(ServiceAdvertiser.class);

    private static ServiceAdvertiser instance;

    private final List<JmDNS> domainNameServices;

    private ServiceAdvertiser() throws InstantiationException {
        this.domainNameServices = new ArrayList<>();

        // skip advertising in debug mode
        if (JPService.debugMode()) {
            return;
        }


//        try { // disabled since its not used yet but causes some network service loops. service might be replaced by another implementation.
//            domainNameServices.add(JmDNS.create());
//        } catch (IOException ex) {
//            ExceptionPrinter.printHistory("Could not initiate domain name service for default interface!", ex, LOGGER);
//        }

//        for (InetAddress localHostLANAddress : getLocalHostLANAddress()) {
//            System.err.println("interface: "+localHostLANAddress.getHostAddress());
//            try {
//                domainNameServices.add(JmDNS.create(localHostLANAddress));
//            } catch (IOException ex) {
//                ExceptionPrinter.printHistory("Could not initiate domain name service for interface: " + localHostLANAddress.getAddress(), ex, LOGGER);
//            }
//        }
    }

    /**
     * Method tries to resolve the best local network interface address that can be used to advertise inet services.
     */
    private static List<InetAddress> getLocalHostLANAddress() {

        final List<InetAddress> siteLocal = new ArrayList<>();
        final List<InetAddress> local = new ArrayList<>();
        final List<InetAddress> loopback = new ArrayList<>();

        try {
            // Iterate all NICs (network interface cards)...
            for (Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces(); ifaces.hasMoreElements(); ) {
                // Iterate all IP addresses assigned to each card...
                for (Enumeration inetAddrs = ifaces.nextElement().getInetAddresses(); inetAddrs.hasMoreElements(); ) {
                    InetAddress inetAddr = (InetAddress) inetAddrs.nextElement();

                    // check if site local
                    if (inetAddr.isSiteLocalAddress()) {
                        siteLocal.add(inetAddr);
                        continue;
                    }

                    // check if loopback
                    if (inetAddr.isLoopbackAddress()) {
                        loopback.add(inetAddr);
                        continue;
                    }

                    // add other
                    local.add(inetAddr);
                }
            }

            // prefer site local addresses
            if (!siteLocal.isEmpty()) {
                System.out.println("found site local: " + siteLocal.size());
                return siteLocal;
            }

            // prefer local if site local is not available
            if (!local.isEmpty()) {
                System.out.println("found local: " + local.size());
                return local;
            }

            // fallback with loopback device
            if (!loopback.isEmpty()) {
                System.out.println("found loopback: " + loopback.size());
                return loopback;
            }
        } catch (final SocketException ex) {
            // try default inet address as fallback
            try {
                local.add(InetAddress.getLocalHost());
            } catch (UnknownHostException e) {
                // otherwise just return empty list.
            }
        }
        return local;
    }

    public static synchronized ServiceAdvertiser getInstance() throws InstantiationException {
        if (instance == null) {
            instance = new ServiceAdvertiser();
        }
        return instance;
    }

    public List<ServiceInfo> register(final HashMap<Fields, String> qualifiedNameMap, final int port, final int weight, final int priority, final boolean persistent, final Map<String, ?> props) throws CouldNotPerformException {
        try {
            final List<ServiceInfo> serviceInfoList = new ArrayList<>();
            for (JmDNS domainNameService : domainNameServices) {

                // Register the service
                final ServiceInfo serviceInfo = ServiceInfo.create(qualifiedNameMap, port, weight, priority, false, props);
                domainNameService.registerService(serviceInfo);
                serviceInfoList.add(serviceInfo);
            }
            return serviceInfoList;
        } catch (IOException ex) {
            throw new CouldNotPerformException("Could not register service!", ex);
        }
    }

    public void deregisterService(final ServiceInfo serviceInfo) {
        for (JmDNS domainNameService : domainNameServices) {
            domainNameService.unregisterService(serviceInfo);
        }
    }

    @Override
    public void shutdown() {
        for (JmDNS domainNameService : domainNameServices) {
            domainNameService.unregisterAllServices();
        }
    }
}
