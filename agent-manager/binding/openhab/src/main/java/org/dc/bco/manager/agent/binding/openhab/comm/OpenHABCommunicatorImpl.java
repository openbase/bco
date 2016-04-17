/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.manager.agent.binding.openhab.comm;

/*
 * #%L
 * COMA DeviceManager Binding OpenHAB
 * %%
 * Copyright (C) 2015 - 2016 DivineCooperation
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
import java.util.HashMap;
import java.util.Map;
import org.dc.bco.dal.lib.jp.JPHardwareSimulationMode;
import org.dc.bco.manager.agent.binding.openhab.execution.OpenHABCommandExecutor;
import org.dc.bco.manager.agent.remote.AgentRemote;
import org.dc.bco.manager.agent.binding.openhab.execution.OpenHABCommandFactory;
import org.dc.bco.registry.agent.remote.AgentRegistryRemote;
import org.dc.jps.core.JPService;
import org.dc.jps.exception.JPNotAvailableException;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.InitializationException;
import org.dc.jul.exception.InstantiationException;
import org.dc.jul.extension.openhab.binding.AbstractOpenHABRemote;
import org.dc.jul.extension.rsb.scope.ScopeGenerator;
import org.dc.jul.pattern.Observable;
import org.dc.jul.pattern.Observer;
import org.dc.jul.processing.StringProcessor;
import rst.homeautomation.control.agent.AgentConfigType.AgentConfig;
import rst.homeautomation.control.agent.AgentDataType.AgentData;
import rst.homeautomation.openhab.OpenhabCommandType.OpenhabCommand;

/**
 * @author thuxohl
 * @author mpohling
 */
public class OpenHABCommunicatorImpl extends AbstractOpenHABRemote {

    private OpenHABCommandExecutor commandExecutor;
    private AgentRegistryRemote agentRegistryRemote;
    private Map<String, AgentRemote> agentRemoteMap;

    public OpenHABCommunicatorImpl() throws InstantiationException, JPNotAvailableException {
        super(JPService.getProperty(JPHardwareSimulationMode.class).getValue());
    }

    @Override
    public void init() throws InitializationException, InterruptedException {
        try {
            this.agentRemoteMap = new HashMap<>();

            this.agentRegistryRemote = new AgentRegistryRemote();
            agentRegistryRemote.init();
            agentRegistryRemote.activate();

            for (AgentConfig agentConfig : agentRegistryRemote.getAgentConfigs()) {
                AgentRemote agentRemote = new AgentRemote();
                agentRemote.addObserver(new Observer<AgentData>() {

                    @Override
                    public void update(Observable<AgentData> source, AgentData data) throws Exception {
                        String itemName = generateItemId(agentRegistryRemote.getAgentConfigById(data.getId()));
                        logger.info("Received update through agent remote for agent [" + data.getLabel() + "]. Executing openhab command...");
                        postCommand(OpenHABCommandFactory.newOnOffCommand(data.getActivationState()).setExecutionType(OpenhabCommand.ExecutionType.UPDATE).setItem(itemName).build());
                    }
                });
                agentRemote.init(agentConfig);
                agentRemote.activate();
                agentRemoteMap.put(agentConfig.getId(), agentRemote);
            }
            this.commandExecutor = new OpenHABCommandExecutor(agentRemoteMap);
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
        super.init();
    }

    private String generateItemId(AgentConfig agentConfig) throws CouldNotPerformException {
        return StringProcessor.transformToIdString("Agent")
                + ITEM_SEGMENT_DELIMITER
                + ScopeGenerator.generateStringRepWithDelimiter(agentConfig.getScope(), ITEM_SUBSEGMENT_DELIMITER);
    }

    @Override
    public void internalReceiveUpdate(final OpenhabCommand command) throws CouldNotPerformException {
        logger.info("Skip received update for agent [" + command.getItem() + "]");
    }

    @Override
    public void internalReceiveCommand(OpenhabCommand command) throws CouldNotPerformException {
        try {
            if (!command.hasItemBindingConfig() || !command.getItemBindingConfig().startsWith("bco.manager.agent")) {
                return; // updated item isn't a scene
            }

            if (!command.hasOnOff() || !command.getOnOff().hasState()) {
                throw new CouldNotPerformException("Command does not have an onOff value required for agents");
            }
            logger.info("Received command for agent [" + command.getItem() + "] from openhab");
            commandExecutor.receiveUpdate(command);
        } catch (Exception ex) {
            throw new CouldNotPerformException("Skip item update [" + command.getItem() + " = " + command.getOnOff() + "]!", ex);
        }
    }
}
