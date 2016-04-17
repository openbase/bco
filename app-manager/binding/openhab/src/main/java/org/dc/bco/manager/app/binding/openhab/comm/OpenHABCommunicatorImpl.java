/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.manager.app.binding.openhab.comm;

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
import org.dc.bco.manager.app.binding.openhab.execution.OpenHABCommandExecutor;
import org.dc.bco.manager.app.binding.openhab.execution.OpenHABCommandFactory;
import org.dc.bco.manager.app.remote.AppRemote;
import org.dc.bco.registry.app.remote.AppRegistryRemote;
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
import rst.homeautomation.control.app.AppConfigType.AppConfig;
import rst.homeautomation.control.app.AppDataType.AppData;
import rst.homeautomation.openhab.OpenhabCommandType.OpenhabCommand;

/**
 * @author thuxohl
 * @author mpohling
 */
public class OpenHABCommunicatorImpl extends AbstractOpenHABRemote {

    private OpenHABCommandExecutor commandExecutor;
    private AppRegistryRemote appRegistryRemote;
    private Map<String, AppRemote> appRemoteMap;

    public OpenHABCommunicatorImpl() throws InstantiationException, JPNotAvailableException {
        super(JPService.getProperty(JPHardwareSimulationMode.class).getValue());
    }

    @Override
    public void init() throws InitializationException, InterruptedException {
        try {
            this.appRemoteMap = new HashMap<>();

            this.appRegistryRemote = new AppRegistryRemote();
            appRegistryRemote.init();
            appRegistryRemote.activate();

            for (AppConfig appConfig : appRegistryRemote.getAppConfigs()) {
                AppRemote appRemote = new AppRemote();
                appRemote.addObserver(new Observer<AppData>() {

                    @Override
                    public void update(Observable<AppData> source, AppData data) throws Exception {
                        String itemName = generateItemId(appRegistryRemote.getAppConfigById(data.getId()));
                        postCommand(OpenHABCommandFactory.newOnOffCommand(data.getActivationState()).setItem(itemName).setExecutionType(OpenhabCommand.ExecutionType.UPDATE).build());
                    }
                });
                appRemote.init(appConfig);
                appRemote.activate();
                appRemoteMap.put(appConfig.getId(), appRemote);
            }
            this.commandExecutor = new OpenHABCommandExecutor(appRemoteMap);
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
        super.init();
    }

    private String generateItemId(AppConfig appConfig) throws CouldNotPerformException {
        return StringProcessor.transformToIdString("App")
                + ITEM_SEGMENT_DELIMITER
                + ScopeGenerator.generateStringRepWithDelimiter(appConfig.getScope(), ITEM_SUBSEGMENT_DELIMITER);
    }

    @Override
    public void internalReceiveUpdate(final OpenhabCommand command) throws CouldNotPerformException {
        logger.info("Skip received update for app [" + command.getItem() + "]");
    }

    @Override
    public void internalReceiveCommand(OpenhabCommand command) throws CouldNotPerformException {
        try {
            if (!command.hasItemBindingConfig() || !command.getItemBindingConfig().startsWith("bco.manager.app")) {
                return; // updated item isn't a app
            }

            if (!command.hasOnOff() || !command.getOnOff().hasState()) {
                throw new CouldNotPerformException("Command does not have an onOff value required for apps.");
            }
            
            logger.info("Received command for app [" + command.getItem() + "] from openhab");
            commandExecutor.receiveUpdate(command);
        } catch (Exception ex) {
            throw new CouldNotPerformException("Skip item update [" + command.getItem() + " = " + command.getOnOff() + "]!", ex);
        }
    }
}
