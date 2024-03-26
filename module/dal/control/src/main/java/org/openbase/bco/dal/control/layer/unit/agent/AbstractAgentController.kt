package org.openbase.bco.dal.control.layer.unit.agent

import org.openbase.bco.dal.control.layer.unit.AbstractAuthorizedBaseUnitController
import org.openbase.bco.dal.lib.layer.unit.agent.AgentController
import org.openbase.bco.registry.remote.Registries
import org.openbase.jul.exception.CouldNotPerformException
import org.openbase.type.domotic.action.ActionParameterType
import org.openbase.type.domotic.unit.UnitConfigType
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig
import org.openbase.type.domotic.unit.agent.AgentDataType

/**
 * @author [Divine Threepwood](mailto:divine@openbase.org)
 */
abstract class AbstractAgentController :
    AbstractAuthorizedBaseUnitController<AgentDataType.AgentData?, AgentDataType.AgentData.Builder?>(
        AgentDataType.AgentData.newBuilder()
    ), AgentController {
    @Throws(InterruptedException::class, CouldNotPerformException::class)
    override fun getActionParameterTemplate(config: UnitConfigType.UnitConfig): ActionParameterType.ActionParameter.Builder {
        val agentClass = Registries.getClassRegistry(true).getAgentClassById(config.agentConfig.agentClassId)
        return ActionParameterType.ActionParameter.newBuilder()
            .addAllCategory(agentClass.categoryList)
            .setPriority(agentClass.priority)
            .setSchedulable(agentClass.schedulable)
            .setInterruptible(agentClass.interruptible)
    }

    override fun isAutostartEnabled(config: UnitConfig?): Boolean =
        config?.agentConfig?.autostart ?: false
}
