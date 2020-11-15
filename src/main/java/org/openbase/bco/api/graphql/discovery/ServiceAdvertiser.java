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
import javax.jmdns.ServiceInfo.Fields;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

public class ServiceAdvertiser implements Shutdownable {

    private static ServiceAdvertiser instance;

    private final JmDNS domainNameService;

    private ServiceAdvertiser() throws InstantiationException {
        try {
            JmDNS domainNameServiceTmp;
            try {
                domainNameServiceTmp = JmDNS.create(getLocalHostLANAddress());
            } catch (CouldNotPerformException e) {
                domainNameServiceTmp = JmDNS.create();
            }
            this.domainNameService = domainNameServiceTmp;
        } catch (IOException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    /**
     * Returns an <code>InetAddress</code> object encapsulating what is most likely the machine's LAN IP address.
     * <p/>
     * This method is intended for use as a replacement of JDK method <code>InetAddress.getLocalHost</code>, because
     * that method is ambiguous on Linux systems. Linux systems enumerate the loopback network interface the same
     * way as regular LAN network interfaces, but the JDK <code>InetAddress.getLocalHost</code> method does not
     * specify the algorithm used to select the address returned under such circumstances, and will often return the
     * loopback address, which is not valid for network communication. Details
     * <a href="http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4665037">here</a>.
     * <p/>
     * This method will scan all IP addresses on all network interfaces on the host machine to determine the IP address
     * most likely to be the machine's LAN address. If the machine has multiple IP addresses, this method will prefer
     * a site-local IP address (e.g. 192.168.x.x or 10.10.x.x, usually IPv4) if the machine has one (and will return the
     * first site-local address if the machine has more than one), but if the machine does not hold a site-local
     * address, this method will return simply the first non-loopback address found (IPv4 or IPv6).
     * <p/>
     * If this method cannot find a non-loopback address using this selection algorithm, it will fall back to
     * calling and returning the result of JDK method <code>InetAddress.getLocalHost</code>.
     * <p/>
     *
     * @throws CouldNotPerformException If the LAN address of the machine cannot be found.
     */
    private static InetAddress getLocalHostLANAddress() throws CouldNotPerformException {
        // method is based on https://stackoverflow.com/questions/9481865/getting-the-ip-address-of-the-current-machine-using-java
        try {
            InetAddress candidateAddress = null;
            // Iterate all NICs (network interface cards)...
            for (Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces(); ifaces.hasMoreElements(); ) {
                // Iterate all IP addresses assigned to each card...
                for (Enumeration inetAddrs = ifaces.nextElement().getInetAddresses(); inetAddrs.hasMoreElements(); ) {
                    InetAddress inetAddr = (InetAddress) inetAddrs.nextElement();

                    // check if loopback
                    if (!inetAddr.isLoopbackAddress()) {

                        if (inetAddr.isSiteLocalAddress()) {
                            // Found non-loopback site-local address. Return it immediately...
                            return inetAddr;
                        } else if (candidateAddress == null) {
                            // Found non-loopback address, but not necessarily site-local.
                            // Store it as a candidate to be returned if site-local address is not subsequently found...
                            candidateAddress = inetAddr;
                            // Note that we don't repeatedly assign non-loopback non-site-local addresses as candidates,
                            // only the first. For subsequent iterations, candidate will be non-null.
                        }
                    }
                }
            }
            if (candidateAddress != null) {
                // We did not find a site-local address, but we found some other non-loopback address.
                // Server might have a non-site-local address assigned to its NIC (or it might be running
                // IPv6 which deprecates the "site-local" concept).
                // Return this non-loopback candidate address...
                return candidateAddress;
            }
            // At this point, we did not find a non-loopback address.
            // Fall back to returning whatever InetAddress.getLocalHost() returns...
            return InetAddress.getLocalHost();
        } catch (Exception ex) {
            throw new CouldNotPerformException("Failed to determine LAN address!", ex);
        }
    }

    public static synchronized ServiceAdvertiser getInstance() throws InstantiationException {
        if (instance == null) {
            instance = new ServiceAdvertiser();
        }
        return instance;
    }

    public ServiceInfo register(final HashMap<Fields, String> qualifiedNameMap, final int port, final int weight, final int priority, final boolean persistent, final Map<String, ?> props) throws CouldNotPerformException {
        try {
            // Register the service
            final ServiceInfo serviceInfo = ServiceInfo.create(qualifiedNameMap, port, weight, priority, false, props);
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
