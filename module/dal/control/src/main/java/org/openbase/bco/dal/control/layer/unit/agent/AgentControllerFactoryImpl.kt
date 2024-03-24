package org.openbase.bco.dal.control.layer.unit.agent

import org.openbase.bco.dal.lib.layer.unit.agent.Agent
import org.openbase.bco.dal.lib.layer.unit.agent.AgentController
import org.openbase.bco.dal.lib.layer.unit.agent.AgentControllerFactory
import org.openbase.bco.registry.remote.Registries
import org.openbase.jul.exception.CouldNotPerformException
import org.openbase.jul.exception.NotAvailableException
import org.openbase.jul.extension.type.processing.LabelProcessor.getBestMatch
import org.openbase.jul.extension.type.processing.LabelProcessor.getLabelByLanguage
import org.openbase.jul.extension.type.processing.MetaConfigVariableProvider
import org.openbase.jul.processing.StringProcessor
import org.openbase.type.domotic.unit.UnitConfigType
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.lang.reflect.InvocationTargetException
import java.util.*

/**
 * @author [Divine Threepwood](mailto:divine@openbase.org)
 */
class AgentControllerFactoryImpl private constructor() : AgentControllerFactory {
    protected val logger: Logger = LoggerFactory.getLogger(AgentControllerFactoryImpl::class.java)

    @Throws(org.openbase.jul.exception.InstantiationException::class)
    override fun newInstance(agentUnitConfig: UnitConfigType.UnitConfig): AgentController {
        var agent: AgentController
        try {
            if (agentUnitConfig == null) {
                throw NotAvailableException("AgentConfig")
            }

            Registries.waitForData()
            val agentClass = Registries.getClassRegistry().getAgentClassById(agentUnitConfig.agentConfig.agentClassId)
            val variableProvider = MetaConfigVariableProvider("AgentClass", agentClass.metaConfig)

            val agentClassPrefix = variableProvider.getValue(
                META_CONFIG_KEY_AGENT_CLASS_PREFIX,
                StringProcessor.removeWhiteSpaces(getLabelByLanguage(Locale.ENGLISH, agentClass.label))
            )

            try {
                // try to load preset agent
                val className = (PRESET_AGENT_PACKAGE_PREFIX
                        + ".agent"
                        + "." + agentClassPrefix + "Agent")
                agent = Thread.currentThread().contextClassLoader.loadClass(className).getConstructor()
                    .newInstance() as AgentController
            } catch (ex: ClassNotFoundException) {
                // try to load custom agent
                val className = (CUSTOM_AGENT_PACKAGE_PREFIX
                        + "." + StringProcessor.removeWhiteSpaces(agentClassPrefix).lowercase(Locale.getDefault())
                        + ".agent"
                        + "." + StringProcessor.transformToPascalCase(StringProcessor.removeWhiteSpaces(agentClassPrefix)) + "Agent")
                agent = Thread.currentThread().contextClassLoader.loadClass(className).getConstructor()
                    .newInstance() as AgentController
            } catch (ex: SecurityException) {
                val className = (CUSTOM_AGENT_PACKAGE_PREFIX
                        + "." + StringProcessor.removeWhiteSpaces(agentClassPrefix).lowercase(Locale.getDefault())
                        + ".agent"
                        + "." + StringProcessor.transformToPascalCase(StringProcessor.removeWhiteSpaces(agentClassPrefix)) + "Agent")
                agent = Thread.currentThread().contextClassLoader.loadClass(className).getConstructor()
                    .newInstance() as AgentController
            } catch (ex: InstantiationException) {
                val className = (CUSTOM_AGENT_PACKAGE_PREFIX
                        + "." + StringProcessor.removeWhiteSpaces(agentClassPrefix).lowercase(Locale.getDefault())
                        + ".agent"
                        + "." + StringProcessor.transformToPascalCase(StringProcessor.removeWhiteSpaces(agentClassPrefix)) + "Agent")
                agent = Thread.currentThread().contextClassLoader.loadClass(className).getConstructor()
                    .newInstance() as AgentController
            } catch (ex: IllegalAccessException) {
                val className = (CUSTOM_AGENT_PACKAGE_PREFIX
                        + "." + StringProcessor.removeWhiteSpaces(agentClassPrefix).lowercase(Locale.getDefault())
                        + ".agent"
                        + "." + StringProcessor.transformToPascalCase(StringProcessor.removeWhiteSpaces(agentClassPrefix)) + "Agent")
                agent = Thread.currentThread().contextClassLoader.loadClass(className).getConstructor()
                    .newInstance() as AgentController
            } catch (ex: IllegalArgumentException) {
                val className = (CUSTOM_AGENT_PACKAGE_PREFIX
                        + "." + StringProcessor.removeWhiteSpaces(agentClassPrefix).lowercase(Locale.getDefault())
                        + ".agent"
                        + "." + StringProcessor.transformToPascalCase(StringProcessor.removeWhiteSpaces(agentClassPrefix)) + "Agent")
                agent = Thread.currentThread().contextClassLoader.loadClass(className).getConstructor()
                    .newInstance() as AgentController
            } catch (ex: NoSuchMethodException) {
                val className = (CUSTOM_AGENT_PACKAGE_PREFIX
                        + "." + StringProcessor.removeWhiteSpaces(agentClassPrefix).lowercase(Locale.getDefault())
                        + ".agent"
                        + "." + StringProcessor.transformToPascalCase(StringProcessor.removeWhiteSpaces(agentClassPrefix)) + "Agent")
                agent = Thread.currentThread().contextClassLoader.loadClass(className).getConstructor()
                    .newInstance() as AgentController
            } catch (ex: InvocationTargetException) {
                val className = (CUSTOM_AGENT_PACKAGE_PREFIX
                        + "." + StringProcessor.removeWhiteSpaces(agentClassPrefix).lowercase(Locale.getDefault())
                        + ".agent"
                        + "." + StringProcessor.transformToPascalCase(StringProcessor.removeWhiteSpaces(agentClassPrefix)) + "Agent")
                agent = Thread.currentThread().contextClassLoader.loadClass(className).getConstructor()
                    .newInstance() as AgentController
            }
            logger.debug("Creating agent of type [" + getBestMatch(agentClass.label) + "]")
            agent.init(agentUnitConfig)
        } catch (ex: CouldNotPerformException) {
            throw org.openbase.jul.exception.InstantiationException(Agent::class.java, agentUnitConfig.id, ex)
        } catch (ex: ClassNotFoundException) {
            throw org.openbase.jul.exception.InstantiationException(Agent::class.java, agentUnitConfig.id, ex)
        } catch (ex: SecurityException) {
            throw org.openbase.jul.exception.InstantiationException(Agent::class.java, agentUnitConfig.id, ex)
        } catch (ex: InstantiationException) {
            throw org.openbase.jul.exception.InstantiationException(Agent::class.java, agentUnitConfig.id, ex)
        } catch (ex: IllegalAccessException) {
            throw org.openbase.jul.exception.InstantiationException(Agent::class.java, agentUnitConfig.id, ex)
        } catch (ex: IllegalArgumentException) {
            throw org.openbase.jul.exception.InstantiationException(Agent::class.java, agentUnitConfig.id, ex)
        } catch (ex: InterruptedException) {
            throw org.openbase.jul.exception.InstantiationException(Agent::class.java, agentUnitConfig.id, ex)
        } catch (ex: NoSuchMethodException) {
            throw org.openbase.jul.exception.InstantiationException(Agent::class.java, agentUnitConfig.id, ex)
        } catch (ex: InvocationTargetException) {
            throw org.openbase.jul.exception.InstantiationException(Agent::class.java, agentUnitConfig.id, ex)
        }
        return agent
    }

    companion object {
        @get:Synchronized
        var instance: AgentControllerFactoryImpl = AgentControllerFactoryImpl()
            private set

        const val META_CONFIG_KEY_AGENT_CLASS_PREFIX: String = "AGENT_CLASS_PREFIX"

        private const val PRESET_AGENT_PACKAGE_PREFIX = "org.openbase.bco.app.preset"
        private const val CUSTOM_AGENT_PACKAGE_PREFIX = "org.openbase.bco.app"
    }
}
