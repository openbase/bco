/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.registry;

import de.citec.dal.hal.device.Device;
import de.citec.dal.hal.device.DeviceFactory;
import de.citec.dm.remote.DeviceRegistryRemote;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.InstantiationException;
import de.citec.jul.exception.VerificationFailedException;
import de.citec.jul.storage.registry.RegistrySynchronizer;
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
        super(registry, remoteRegistry.getDeviceConfigRemoteRegistry(), new DeviceFactory(remoteRegistry));
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
