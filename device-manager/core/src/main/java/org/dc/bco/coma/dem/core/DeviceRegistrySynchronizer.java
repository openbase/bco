/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.coma.dem.core;

import org.dc.bco.coma.dem.lib.Device;
import org.dc.bco.coma.dem.core.DeviceFactoryImpl;
import org.dc.bco.registry.device.remote.DeviceRegistryRemote;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.InstantiationException;
import org.dc.jul.exception.VerificationFailedException;
import org.dc.jul.storage.registry.RegistrySynchronizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.homeautomation.binding.BindingTypeHolderType;
import rst.homeautomation.device.DeviceClassType.DeviceClass;
import rst.homeautomation.device.DeviceConfigType.DeviceConfig;
import rst.homeautomation.state.InventoryStateType;

/** 
 *
 * @author mpohling
 */
public class DeviceRegistrySynchronizer extends RegistrySynchronizer<String, Device, DeviceConfig, DeviceConfig.Builder> {

    private static final Logger logger = LoggerFactory.getLogger(DeviceRegistrySynchronizer.class);

private final DeviceRegistryRemote remoteRegistry;

    public DeviceRegistrySynchronizer(final DeviceRegistry registry, final DeviceRegistryRemote remoteRegistry) throws InstantiationException {
        super(registry, remoteRegistry.getDeviceConfigRemoteRegistry(), new DeviceFactoryImpl(remoteRegistry));
        try {
            this.remoteRegistry = remoteRegistry;
        } catch (Exception ex) {
            throw new InstantiationException(this, ex);
        }
    }

    @Override
    public boolean verifyConfig(final DeviceConfig config) throws VerificationFailedException {
        try {

            // load device class
            DeviceClass deviceClass;
            try {
                deviceClass = remoteRegistry.getDeviceClassById(config.getDeviceClassId());
            } catch (CouldNotPerformException ex) {
                throw new VerificationFailedException("Could not load device class of Device[" + config.getId() + "] !", ex);
            }

            if (!deviceClass.getBindingConfig().getType().equals(BindingTypeHolderType.BindingTypeHolder.BindingType.OPENHAB)) {
                // TODO mpohling: check all dal supported binding types.
                return false;
            }

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
