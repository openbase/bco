package org.openbase.bco.dal.control.layer.unit.agent;

/*
 * #%L
 * BCO DAL Control
 * %%
 * Copyright (C) 2014 - 2021 openbase.org
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

import org.openbase.bco.dal.lib.layer.unit.agent.Agent;
import org.openbase.bco.dal.lib.layer.unit.agent.AgentController;
import org.openbase.bco.dal.lib.layer.unit.agent.AgentControllerFactory;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.type.processing.LabelProcessor;
import org.openbase.jul.extension.type.processing.MetaConfigVariableProvider;
import org.openbase.jul.processing.StringProcessor;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.domotic.unit.agent.AgentClassType.AgentClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.Locale;

/**
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class AgentControllerFactoryImpl implements AgentControllerFactory {

    protected final Logger logger = LoggerFactory.getLogger(AgentControllerFactoryImpl.class);
    private static AgentControllerFactoryImpl instance;

    public static final String META_CONFIG_KEY_AGENT_CLASS_PREFIX = "AGENT_CLASS_PREFIX";

    private static final String PRESET_AGENT_PACKAGE_PREFIX = "org.openbase.bco.app.preset";
    private static final String CUSTOM_AGENT_PACKAGE_PREFIX = "org.openbase.bco.app";

    public synchronized static AgentControllerFactoryImpl getInstance() {
        if (instance == null) {
            instance = new AgentControllerFactoryImpl();
        }
        return instance;
    }

    private AgentControllerFactoryImpl() {
    }

    @Override
    public AgentController newInstance(final UnitConfig agentUnitConfig) throws org.openbase.jul.exception.InstantiationException {
        AgentController agent;
        try {
            if (agentUnitConfig == null) {
                throw new NotAvailableException("AgentConfig");
            }

            Registries.waitForData();
            final AgentClass agentClass = Registries.getClassRegistry().getAgentClassById(agentUnitConfig.getAgentConfig().getAgentClassId());
            final MetaConfigVariableProvider variableProvider = new MetaConfigVariableProvider("AgentClass", agentClass.getMetaConfig());

            final String agentClassPrefix = variableProvider.getValue(
                    META_CONFIG_KEY_AGENT_CLASS_PREFIX,
                    StringProcessor.removeWhiteSpaces(LabelProcessor.getLabelByLanguage(Locale.ENGLISH, agentClass.getLabel())));

            try {
                // try to load preset agent
                String className = PRESET_AGENT_PACKAGE_PREFIX
                        + ".agent"
                        + "." + agentClassPrefix + "Agent";
                agent = (AgentController) Thread.currentThread().getContextClassLoader().loadClass(className).getConstructor().newInstance();
            } catch (ClassNotFoundException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | NoSuchMethodException | InvocationTargetException ex) {
                // try to load custom agent
                String className = CUSTOM_AGENT_PACKAGE_PREFIX
                        + "." + StringProcessor.removeWhiteSpaces(agentClassPrefix).toLowerCase()
                        + ".agent"
                        + "." + StringProcessor.transformToPascalCase(StringProcessor.removeWhiteSpaces(agentClassPrefix)) + "Agent";
                agent = (AgentController) Thread.currentThread().getContextClassLoader().loadClass(className).getConstructor().newInstance();
            }
            logger.debug("Creating agent of type [" + LabelProcessor.getBestMatch(agentClass.getLabel()) + "]");
            agent.init(agentUnitConfig);
        } catch (CouldNotPerformException | ClassNotFoundException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | InterruptedException | NoSuchMethodException | InvocationTargetException ex) {
            throw new org.openbase.jul.exception.InstantiationException(Agent.class, agentUnitConfig.getId(), ex);
        }
        return agent;
    }
}
