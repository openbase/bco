/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.manager.scene.binding.openhab.comm;

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
import org.dc.bco.manager.scene.binding.openhab.execution.OpenHABCommandExecutor;
import org.dc.bco.manager.scene.binding.openhab.execution.OpenHABCommandFactory;
import org.dc.bco.manager.scene.remote.SceneRemote;
import org.dc.bco.registry.scene.remote.SceneRegistryRemote;
import org.dc.jps.core.JPService;
import org.dc.jps.exception.JPNotAvailableException;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.InitializationException;
import org.dc.jul.exception.InstantiationException;
import org.dc.jul.extension.openhab.binding.AbstractOpenHABCommunicator;
import org.dc.jul.extension.rsb.scope.ScopeGenerator;
import org.dc.jul.pattern.Observable;
import org.dc.jul.pattern.Observer;
import org.dc.jul.processing.StringProcessor;
import rst.homeautomation.control.scene.SceneConfigType.SceneConfig;
import rst.homeautomation.control.scene.SceneDataType;
import rst.homeautomation.openhab.OpenhabCommandType.OpenhabCommand;

/**
 * @author thuxohl
 * @author mpohling
 */
public class OpenHABCommunicatorImpl extends AbstractOpenHABCommunicator {

    private OpenHABCommandExecutor commandExecutor;
    private SceneRegistryRemote sceneRegistryRemote;
    private Map<String, SceneRemote> sceneRemoteMap;

    public OpenHABCommunicatorImpl() throws InstantiationException, JPNotAvailableException {
        super(JPService.getProperty(JPHardwareSimulationMode.class).getValue());
    }

    @Override
    public void init() throws InitializationException, InterruptedException {
        try {
            this.sceneRemoteMap = new HashMap<>();

            this.sceneRegistryRemote = new SceneRegistryRemote();
            sceneRegistryRemote.init();
            sceneRegistryRemote.activate();

            for (SceneConfig sceneConfig : sceneRegistryRemote.getSceneConfigs()) {
                SceneRemote sceneRemote = new SceneRemote();
                sceneRemote.addObserver(new Observer<SceneDataType.SceneData>() {

                    @Override
                    public void update(Observable<SceneDataType.SceneData> source, SceneDataType.SceneData data) throws Exception {
                        logger.info("Got new data for scene [" + data.getLabel() + "]");
                        String itemName = generateItemId(sceneRegistryRemote.getSceneConfigById(data.getId()));
                        executeCommand(OpenHABCommandFactory.newOnOffCommand(data.getActivationState()).setItem(itemName).setExecutionType(OpenhabCommand.ExecutionType.UPDATE).build());
                    }
                });
                sceneRemote.init(sceneConfig);
                sceneRemote.activate();
                sceneRemoteMap.put(sceneConfig.getId(), sceneRemote);
            }
            this.commandExecutor = new OpenHABCommandExecutor(sceneRemoteMap);
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
        super.init();
    }

    private String generateItemId(SceneConfig sceneConfig) throws CouldNotPerformException {
        return StringProcessor.transformToIdString("Scene")
                + ITEM_SEGMENT_DELIMITER
                + ScopeGenerator.generateStringRepWithDelimiter(sceneConfig.getScope(), ITEM_SUBSEGMENT_DELIMITER);
    }

    @Override
    public void internalReceiveUpdate(final OpenhabCommand command) throws CouldNotPerformException {
        logger.info("Skip received update for scene [" + command.getItem() + "]");
    }

    @Override
    public void internalReceiveCommand(OpenhabCommand command) throws CouldNotPerformException {
        try {
            if (!command.hasOnOff() || !command.getOnOff().hasState()) {
                throw new CouldNotPerformException("Command does not have an onOff value required for scenes");
            }
            if (!command.hasItemBindingConfig() || !command.getItemBindingConfig().startsWith("bco.manager.scene")) {
                return; // updated item isn't a scene
            }
            logger.info("Received command for scene [" + command.getItem() + "] from openhab");
            commandExecutor.receiveUpdate(command);
        } catch (Exception ex) {
            throw new CouldNotPerformException("Skip item update [" + command.getItem() + " = " + command.getOnOff() + "]!", ex);
        }
    }
}
