package org.openbase.bco.dal.control.layer.unit.agent

import org.openbase.bco.authentication.lib.BCO
import org.openbase.bco.dal.lib.layer.unit.agent.AgentManager
import org.openbase.jul.pattern.launch.AbstractLauncher

class AgentManagerLauncher :
    AbstractLauncher<AgentManagerImpl>(AgentManager::class.java, AgentManagerImpl::class.java) {
    override fun loadProperties() {
    }

    companion object {
        /**
         * @param args the command line arguments
         */
        @JvmStatic
        fun main(args: Array<String>) {
            BCO.printLogo()
            main(BCO::class.java, AgentManager::class.java, args, AgentManagerLauncher::class.java)
        }
    }
}
