package org.openbase.bco.api.graphql

import org.openbase.bco.api.graphql.discovery.ServiceAdvertiser
import org.openbase.bco.registry.remote.Registries
import org.openbase.bco.registry.remote.login.BCOLogin
import org.openbase.jps.core.JPService
import org.openbase.jul.exception.*
import org.openbase.jul.iface.Launchable
import org.openbase.jul.iface.VoidInitializable
import org.slf4j.LoggerFactory
import org.springframework.boot.SpringApplication
import org.springframework.context.ConfigurableApplicationContext
import java.util.*
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
 */   class BcoApiGraphQlSpringLaunchable : Launchable<Void>, VoidInitializable {
    private var serviceAdvertiser: ServiceAdvertiser? = null
    private var context: ConfigurableApplicationContext? = null

    @Throws(InitializationException::class)
    override fun init() {
        serviceAdvertiser = try {
            ServiceAdvertiser.instance
        } catch (ex: InstantiationException) {
            throw InitializationException(this, ex)
        }
    }

    @Throws(CouldNotPerformException::class, InterruptedException::class)
    override fun activate() {
        LOGGER.info("Connect to bco...")
        Registries.waitUntilReady()
        LOGGER.info("Login to bco...")
        BCOLogin.getSession().loginBCOUser()
        LOGGER.info("Start webserver...")
        context = SpringApplication.run(BcoGraphQlApiSpringBootApplication::class.java, *JPService.getArgs())
        LOGGER.info("Advertise graphql service...")
        val qualifiedNameMap = HashMap<ServiceInfo.Fields, String>()
        qualifiedNameMap[ServiceInfo.Fields.Application] = "http"
        qualifiedNameMap[ServiceInfo.Fields.Instance] = "graphql-bco-openbase"
        qualifiedNameMap[ServiceInfo.Fields.Subtype] = "graphql"
        val propertyMap = HashMap<String, String>()
        propertyMap["bco-uuid"] = UUID.randomUUID().toString()
        propertyMap["path"] = "graphql"

        // lookup port
        context?.getEnvironment()?.getProperty("server.port")?.toInt()?.let { port ->
            // register service advertising
            serviceAdvertiser!!.register(qualifiedNameMap, port, 0, 0, false, propertyMap)
        }
    }

    override fun deactivate() {
        LOGGER.info("Logout...")
        serviceAdvertiser!!.shutdown()
        BCOLogin.getSession().logout()
        if (isActive) {
            LOGGER.info("Shutdown " + context!!.applicationName)
            SpringApplication.exit(context)
            context = null
        }
    }

    override fun isActive(): Boolean {
        return context != null && context!!.isActive
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(BcoApiGraphQlSpringLaunchable::class.java)
    }
}
