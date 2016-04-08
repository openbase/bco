/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.manager.scene.binding.openhab.execution;

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
//import org.dc.bco.manager.device.binding.openhab.transform.OpenhabCommandTransformer;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.bco.manager.scene.binding.openhab.transform.ActivationStateTransformer;
import org.dc.bco.manager.scene.remote.SceneRemote;
import org.dc.bco.registry.scene.lib.SceneRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.homeautomation.openhab.OpenhabCommandType;
import rst.homeautomation.openhab.OpenhabCommandType.OpenhabCommand;

/**
 *
 * @author Divine Threepwood
 */
public class OpenHABCommandExecutor {

    private static final Logger logger = LoggerFactory.getLogger(OpenHABCommandExecutor.class);

    private final SceneRegistry sceneRegistry;

    public OpenHABCommandExecutor(SceneRegistry sceneRegistry) {
        this.sceneRegistry = sceneRegistry;
    }

    private class OpenhabCommandMetaData {

        private final OpenhabCommandType.OpenhabCommand command;
        private final String sceneId;

        public OpenhabCommandMetaData(OpenhabCommand command) throws CouldNotPerformException {
            this.command = command;
            try {
                sceneId = command.getItem().split(":")[1];
            } catch (ArrayIndexOutOfBoundsException ex) {
                throw new CouldNotPerformException("Could not extract sceneId from command item config!", ex);
            }
        }

        public OpenhabCommand getCommand() {
            return command;
        }

        public String getSceneId() {
            return sceneId;
        }
    }

    public void receiveUpdate(OpenhabCommandType.OpenhabCommand command) throws CouldNotPerformException {
        logger.info("receiveUpdate [" + command.getItem() + "=" + command.getType() + "]");

        OpenhabCommandMetaData metaData = new OpenhabCommandMetaData(command);
        SceneRemote sceneRemote = new SceneRemote();

        try {
            sceneRemote.init(sceneRegistry.getSceneConfigById(metaData.getSceneId()));
            sceneRemote.activate();
            sceneRemote.setActivationState(ActivationStateTransformer.transform(command.getOnOff().getState()));
        } catch (InterruptedException ex) {
            throw new CouldNotPerformException("Could not init/activate scene remote", ex);
        }

        //TODO: shutdown sceneRemote? or use a map for every sceneId a remote?
    }
}
