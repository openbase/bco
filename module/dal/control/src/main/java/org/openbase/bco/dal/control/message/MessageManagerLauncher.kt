package org.openbase.bco.dal.control.message

import org.openbase.bco.authentication.lib.BCO
import org.openbase.bco.authentication.lib.jp.JPBCODistributionDirectory
import org.openbase.bco.dal.lib.jp.JPBenchmarkMode
import org.openbase.bco.dal.lib.jp.JPHardwareSimulationMode
import org.openbase.bco.dal.lib.jp.JPProviderControlMode
import org.openbase.jps.core.JPService
import org.openbase.jul.pattern.launch.AbstractLauncher

class MessageManagerLauncher :
    AbstractLauncher<MessageManager>(MessageManager::class.java, MessageManager::class.java) {
    public override fun loadProperties() {
        JPService.registerProperty(JPBCODistributionDirectory::class.java)
        JPService.registerProperty(JPHardwareSimulationMode::class.java)
        JPService.registerProperty(JPBenchmarkMode::class.java)
        JPService.registerProperty(JPProviderControlMode::class.java)
    }

    companion object {
        @Throws(Throwable::class)
        @JvmStatic
        fun main(args: Array<String>) {
            BCO.printLogo()
            main(
                BCO::class.java,
                MessageManager::class.java, args,
                MessageManagerLauncher::class.java
            )
        }
    }
}
