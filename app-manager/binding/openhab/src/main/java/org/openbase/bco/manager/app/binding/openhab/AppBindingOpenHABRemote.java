/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openbase.bco.manager.app.binding.openhab;

/*-
 * #%L
 * BCO Manager App Binding OpenHAB
 * %%
 * Copyright (C) 2015 - 2018 openbase.org
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

import org.openbase.bco.dal.remote.unit.app.AppRemote;
import org.openbase.bco.manager.app.binding.openhab.transform.ActivationStateTransformer;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.extension.openhab.binding.AbstractOpenHABRemote;
import org.openbase.jul.storage.registry.RemoteControllerRegistryImpl;
import rst.domotic.binding.openhab.OpenhabCommandType;

/**
 *
 * @author pleminoq
 */
public class AppBindingOpenHABRemote extends AbstractOpenHABRemote {

    private final RemoteControllerRegistryImpl<String, AppRemote> appRegistry;

    public AppBindingOpenHABRemote(final boolean hardwareSimulationMode, final RemoteControllerRegistryImpl<String, AppRemote> appRegistry) {
        super(hardwareSimulationMode);

        this.appRegistry = appRegistry;
    }

    @Override
    public void internalReceiveUpdate(OpenhabCommandType.OpenhabCommand command) throws CouldNotPerformException {
        logger.debug("Ignore update for app manager openhab binding.");
    }

    @Override
    public void internalReceiveCommand(OpenhabCommandType.OpenhabCommand command) throws CouldNotPerformException {
        try {

            if (!command.hasOnOff() || !command.getOnOff().hasState()) {
                throw new CouldNotPerformException("Command does not have an onOff value required for apps");
            }
            logger.debug("Received command for app [" + command.getItem() + "] from openhab");
            appRegistry.get(getIdFromOpenHABItem(command)).setActivationState(ActivationStateTransformer.transform(command.getOnOff().getState()));
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Skip item update [" + command.getItem() + " = " + command.getOnOff() + "]!", ex);
        }
    }

    private String getIdFromOpenHABItem(OpenhabCommandType.OpenhabCommand command) {
        return command.getItemBindingConfig().split(":")[1];
    }
}
