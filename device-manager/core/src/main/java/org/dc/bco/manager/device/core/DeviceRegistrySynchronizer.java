/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.manager.device.core;

/*
 * #%L
 * COMA DeviceManager Core
 * %%
 * Copyright (C) 2015 - 2016 DivineCooperation
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import org.dc.bco.manager.device.lib.Device;
import org.dc.bco.manager.device.lib.DeviceFactory;
import org.dc.bco.registry.device.remote.DeviceRegistryRemote;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.InstantiationException;
import org.dc.jul.exception.VerificationFailedException;
import org.dc.jul.storage.registry.RegistrySynchronizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.homeautomation.device.DeviceConfigType.DeviceConfig;
import rst.homeautomation.state.InventoryStateType;

/** 
 *
 * @author mpohling
 */
public class DeviceRegistrySynchronizer extends RegistrySynchronizer<String, Device, DeviceConfig, DeviceConfig.Builder> {

    private static final Logger logger = LoggerFactory.getLogger(DeviceRegistrySynchronizer.class);

    private final DeviceRegistryRemote remoteRegistryRemote;
    final DeviceManagerController deviceManagerController;

    public DeviceRegistrySynchronizer(final DeviceManagerController deviceManagerController, final DeviceFactory deviceFactory) throws InstantiationException {
        super(deviceManagerController.getDeviceControllerRegistry(), deviceManagerController.getDeviceRegistry().getDeviceConfigRemoteRegistry(), deviceFactory);
//        try {
            this.deviceManagerController = deviceManagerController;
            this.remoteRegistryRemote = deviceManagerController.getDeviceRegistry();
//        } catch (CouldNotPerformException ex) {
//            throw new InstantiationException(this, ex);
//        }
    }

    @Override
    public boolean verifyConfig(final DeviceConfig config) throws VerificationFailedException {
        try {

            // verify device class.
            try {
                remoteRegistryRemote.containsDeviceClassById(config.getDeviceClassId());
            } catch (CouldNotPerformException ex) {
                throw new VerificationFailedException("DeviceClass["+config.getDeviceClassId()+"] of Device[" + config.getId() + "] is not supported yet!", ex);
            }

            // verify device manager support.
            if(!deviceManagerController.isSupported(config)) {
                return false;
            }

            // verify device state.
            if (config.getInventoryState().getValue() != InventoryStateType.InventoryState.State.INSTALLED) {
                logger.info("Skip Device[" + config.getLabel() + "] because it is currently not installed!");
                return false;
            }
            return true;
        } catch (CouldNotPerformException ex) {
            throw new VerificationFailedException("Could not verify device config!", ex);
        }
    }
}
