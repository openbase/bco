package org.openbase.bco.dal.control.layer.unit.agent

import org.openbase.bco.dal.control.layer.unit.UnitControllerRegistrySynchronizer
import org.openbase.bco.dal.lib.layer.unit.UnitControllerRegistry
import org.openbase.bco.dal.lib.layer.unit.UnitControllerRegistryImpl
import org.openbase.bco.dal.lib.layer.unit.agent.AgentController
import org.openbase.bco.dal.lib.layer.unit.agent.AgentControllerFactory
import org.openbase.bco.dal.lib.layer.unit.agent.AgentManager
import org.openbase.bco.registry.remote.Registries
import org.openbase.bco.registry.remote.login.BCOLogin
import org.openbase.jul.exception.CouldNotPerformException
import org.openbase.jul.exception.InstantiationException
import org.openbase.jul.iface.Launchable
import org.openbase.jul.iface.VoidInitializable
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class AgentManagerImpl : AgentManager, Launchable<Void?>, VoidInitializable {
    private var factory: AgentControllerFactory
    private var agentControllerRegistry: UnitControllerRegistry<AgentController>? = null
    private var agentRegistrySynchronizer: UnitControllerRegistrySynchronizer<AgentController>? = null

    init {
        try {
            this.factory = AgentControllerFactoryImpl.instance
            this.agentControllerRegistry = UnitControllerRegistryImpl()
            this.agentRegistrySynchronizer = UnitControllerRegistrySynchronizer(
                agentControllerRegistry,
                Registries.getUnitRegistry().getAgentUnitConfigRemoteRegistry(false),
                factory
            )
        } catch (ex: CouldNotPerformException) {
            throw InstantiationException(this, ex)
        }
    }

    override fun init() {
        // this has to stay, else do not implement VoidInitializable
    }

    @Throws(CouldNotPerformException::class, InterruptedException::class)
    override fun activate() {
        BCOLogin.getSession().loginBCOUser()
        agentControllerRegistry!!.activate()
        agentRegistrySynchronizer!!.activate()
    }

    override fun isActive(): Boolean {
        return agentRegistrySynchronizer!!.isActive &&
                agentControllerRegistry!!.isActive
    }

    @Throws(CouldNotPerformException::class, InterruptedException::class)
    override fun deactivate() {
        agentRegistrySynchronizer!!.deactivate()
        agentControllerRegistry!!.deactivate()
    }

    override fun shutdown() {
        agentRegistrySynchronizer!!.shutdown()
        agentControllerRegistry!!.shutdown()
    }

    override fun getAgentControllerRegistry(): UnitControllerRegistry<AgentController> {
        return agentControllerRegistry!!
    }

    companion object {
        protected val LOGGER: Logger = LoggerFactory.getLogger(AgentManagerImpl::class.java)
    }
}
