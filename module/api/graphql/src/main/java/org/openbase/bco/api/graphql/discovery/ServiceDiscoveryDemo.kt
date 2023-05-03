package org.openbase.bco.api.graphql.discovery

import org.openbase.jul.processing.StringProcessor
import java.io.IOException
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.*
import javax.jmdns.JmDNS
import javax.jmdns.ServiceEvent
import javax.jmdns.ServiceListener

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
 */   object ServiceDiscoveryDemo {
    @Throws(InterruptedException::class)
    fun disabledMain(args: Array<String>?) {
        try {
            // Create a JmDNS instance
            val jmdns = JmDNS.create(InetAddress.getLocalHost())

            // Add a service listener
            jmdns.addServiceListener("_http._tcp.local.", SampleListener("graphql-bco-openbase"))

            // Wait a bit
            Thread.sleep(30000)
        } catch (e: UnknownHostException) {
            println(e.message)
        } catch (e: IOException) {
            println(e.message)
        }
    }

    private class SampleListener : ServiceListener {
        private val serviceNameFilter: String?

        constructor(serviceNameFilter: String?) {
            this.serviceNameFilter = serviceNameFilter
        }

        constructor() {
            serviceNameFilter = null
        }

        override fun serviceAdded(event: ServiceEvent) {}
        override fun serviceRemoved(event: ServiceEvent) {
            if (serviceNameFilter == null || event.name == serviceNameFilter) {
                println(
                    "Offline: " + event.name + "@" + event.info.server + ":" + event.info.port + " - " + StringProcessor.transformCollectionToString(
                        Arrays.asList(*event.info.hostAddresses.clone()), ", "
                    )
                )
            }
        }

        override fun serviceResolved(event: ServiceEvent) {
            if (serviceNameFilter == null || event.name == serviceNameFilter) {
                println(
                    "Online: " + event.name + "@" + event.info.server + ":" + event.info.port + " - " + StringProcessor.transformCollectionToString(
                        Arrays.asList(*event.info.hostAddresses.clone()), ", "
                    )
                )
            }
        }
    }
}
