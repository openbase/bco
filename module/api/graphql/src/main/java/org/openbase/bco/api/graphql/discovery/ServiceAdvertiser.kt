package org.openbase.bco.api.graphql.discovery

import org.openbase.jps.core.JPService
import org.openbase.jul.exception.CouldNotPerformException
import org.openbase.jul.exception.InstantiationException
import org.openbase.jul.iface.Shutdownable
import org.slf4j.LoggerFactory
import java.io.IOException
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.SocketException
import java.net.UnknownHostException
import java.util.*
import javax.jmdns.JmDNS
import javax.jmdns.ServiceInfo


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
 */   class ServiceAdvertiser private constructor() : Shutdownable {
    private val domainNameServices: List<JmDNS>

    init {
        domainNameServices = ArrayList()

        // skip advertising in debug mode
        if (!JPService.debugMode()) {
            // disabled since its not used yet but causes some network service loops. service might be replaced by another implementation.
//            try { domainNameServices.add(
//                JmDNS.create())
//            } catch (ex: IOException) {
//                ExceptionPrinter.printHistory(
//                    "Could not initiate domain name service for default interface!",
//                    ex,
//                    LOGGER
//                )
//            }
//
//            for (localHostLANAddress in getLocalHostLANAddress()) {
//                System.err.println("interface: " + localHostLANAddress.hostAddress)
//                try {
//                    domainNameServices.add(JmDNS.create(localHostLANAddress))
//                } catch (ex: IOException) {
//                    ExceptionPrinter.printHistory(
//                        "Could not initiate domain name service for interface: " + localHostLANAddress.address,
//                        ex,
//                        LOGGER
//                    )
//                }
//            }
        }
    }

    @Throws(CouldNotPerformException::class)
    fun register(
        qualifiedNameMap: HashMap<ServiceInfo.Fields, String>?,
        port: Int,
        weight: Int,
        priority: Int,
        persistent: Boolean,
        props: Map<String, *>?,
    ): List<ServiceInfo> {
        return try {
            val serviceInfoList: MutableList<ServiceInfo> = ArrayList()
            for (domainNameService in domainNameServices) {

                // Register the service
                val serviceInfo = ServiceInfo.create(qualifiedNameMap, port, weight, priority, false, props)
                domainNameService.registerService(serviceInfo)
                serviceInfoList.add(serviceInfo)
            }
            serviceInfoList
        } catch (ex: IOException) {
            throw CouldNotPerformException("Could not register service!", ex)
        }
    }

    fun deregisterService(serviceInfo: ServiceInfo?) {
        for (domainNameService in domainNameServices) {
            domainNameService.unregisterService(serviceInfo)
        }
    }

    override fun shutdown() {
        for (domainNameService in domainNameServices) {
            domainNameService.unregisterAllServices()
        }
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(ServiceAdvertiser::class.java)


        @get:Throws(InstantiationException::class)
        @get:Synchronized
        val instance: ServiceAdvertiser by lazy { ServiceAdvertiser() }

        private val localHostLANAddress: List<InetAddress>
            /**
             * Method tries to resolve the best local network interface address that can be used to advertise inet services.
             */
            private get() {
                val siteLocal: MutableList<InetAddress> = ArrayList()
                val local: MutableList<InetAddress> = ArrayList()
                val loopback: MutableList<InetAddress> = ArrayList()
                try {
                    // Iterate all NICs (network interface cards)...
                    val ifaces = NetworkInterface.getNetworkInterfaces()
                    while (ifaces.hasMoreElements()) {

                        // Iterate all IP addresses assigned to each card...
                        val inetAddrs: Enumeration<*> = ifaces.nextElement().inetAddresses
                        while (inetAddrs.hasMoreElements()) {
                            val inetAddr = inetAddrs.nextElement() as InetAddress

                            // check if site local
                            if (inetAddr.isSiteLocalAddress) {
                                siteLocal.add(inetAddr)
                                continue
                            }

                            // check if loopback
                            if (inetAddr.isLoopbackAddress) {
                                loopback.add(inetAddr)
                                continue
                            }

                            // add other
                            local.add(inetAddr)
                        }
                    }

                    // prefer site local addresses
                    if (!siteLocal.isEmpty()) {
                        println("found site local: " + siteLocal.size)
                        return siteLocal
                    }

                    // prefer local if site local is not available
                    if (!local.isEmpty()) {
                        println("found local: " + local.size)
                        return local
                    }

                    // fallback with loopback device
                    if (!loopback.isEmpty()) {
                        println("found loopback: " + loopback.size)
                        return loopback
                    }
                } catch (ex: SocketException) {
                    // try default inet address as fallback
                    try {
                        local.add(InetAddress.getLocalHost())
                    } catch (e: UnknownHostException) {
                        // otherwise just return empty list.
                    }
                }
                return local
            }
    }
}
