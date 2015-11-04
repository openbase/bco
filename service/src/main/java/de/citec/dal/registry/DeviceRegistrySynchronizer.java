/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.registry;

import de.citec.dal.hal.device.DeviceFactory;
import de.citec.dm.remote.DeviceRegistryRemote;
import de.citec.jps.core.JPService;
import de.citec.jps.preset.JPTestMode;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.printer.ExceptionPrinter;
import de.citec.jul.exception.InstantiationException;
import de.citec.jul.exception.MultiException;
import de.citec.jul.exception.printer.LogLevel;
import de.citec.jul.extension.protobuf.ProtobufListDiff;
import de.citec.jul.pattern.Observable;
import de.citec.jul.pattern.Observer;
import de.citec.jul.schedule.RecurrenceEventFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.homeautomation.binding.BindingTypeHolderType;
import rst.homeautomation.device.DeviceClassType.DeviceClass;
import rst.homeautomation.device.DeviceConfigType.DeviceConfig;
import rst.homeautomation.device.DeviceRegistryType;
import rst.homeautomation.state.InventoryStateType;

/**
 *
 * @author mpohling
 */
public class DeviceRegistrySynchronizer {

    private static final Logger logger = LoggerFactory.getLogger(DeviceRegistrySynchronizer.class);
    private final DeviceFactory factory;
    private final DeviceRegistry registry;
    private final DeviceRegistryRemote remoteRegistry;
    private final Observer<DeviceRegistryType.DeviceRegistry> remoteChangeObserver;
    private final RecurrenceEventFilter recurrenceSyncFilter;
    private final ProtobufListDiff<String, DeviceConfig, DeviceConfig.Builder> deviceConfigDiff;

    public DeviceRegistrySynchronizer(final DeviceRegistry registry, final DeviceRegistryRemote remoteRegistry) throws InstantiationException {
        try {
            this.factory = new DeviceFactory(remoteRegistry);
            this.registry = registry;
            this.remoteRegistry = remoteRegistry;
            this.deviceConfigDiff = new ProtobufListDiff<>();
            this.recurrenceSyncFilter = new RecurrenceEventFilter(15000) {

                @Override
                public void relay() throws Exception {
                    internalSync();
                }
            };

            this.remoteChangeObserver = new Observer<DeviceRegistryType.DeviceRegistry>() {

                @Override
                public void update(Observable<DeviceRegistryType.DeviceRegistry> source, DeviceRegistryType.DeviceRegistry data) throws Exception {
                    sync();
                }
            };
        } catch (Exception ex) {
            throw new InstantiationException(this, ex);
        }
    }

    public void init() throws CouldNotPerformException {
        this.remoteRegistry.addObserver(remoteChangeObserver);
        try {
            this.internalSync();
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Initial sync failed!", ex), logger, LogLevel.ERROR);
            if (JPService.getProperty(JPTestMode.class).getValue()) {
                throw ex;
            }
        }
    }

    public void shutdown() {
        this.remoteRegistry.removeObserver(remoteChangeObserver);
        this.recurrenceSyncFilter.cancel();
    }

    private void sync() {
        recurrenceSyncFilter.trigger();
    }

    private synchronized void internalSync() throws CouldNotPerformException {
        logger.info("Perform registry sync...");

        try {
            deviceConfigDiff.diff(remoteRegistry.getDeviceConfigs());

            MultiException.ExceptionStack removeExceptionStack = null;
            for (DeviceConfig config : deviceConfigDiff.getRemovedMessageMap().getMessages()) {
                try {
                    registry.remove(config.getId());
                } catch (Exception ex) {
                    removeExceptionStack = MultiException.push(this, ex, removeExceptionStack);
                }
            }

            MultiException.ExceptionStack updateExceptionStack = null;
            for (DeviceConfig config : deviceConfigDiff.getUpdatedMessageMap().getMessages()) {
                try {
                    registry.update(factory.newInstance(config));
                } catch (Exception ex) {
                    updateExceptionStack = MultiException.push(this, ex, updateExceptionStack);
                }
            }

            MultiException.ExceptionStack registerExceptionStack = null;
            for (DeviceConfig config : deviceConfigDiff.getNewMessageMap().getMessages()) {
                try {
                    if (verifyDeviceConfig(config)) {
                        registry.register(factory.newInstance(config));
                    }
                } catch (Exception ex) {
                    registerExceptionStack = MultiException.push(this, ex, registerExceptionStack);
                }
            }
            
            int errorCounter = MultiException.size(removeExceptionStack) + MultiException.size(updateExceptionStack) + MultiException.size(registerExceptionStack);
            logger.info(deviceConfigDiff.getChangeCounter()+ " registry changes applied. " + errorCounter + " are skipped.");

            // build exception cause chain.
            MultiException.ExceptionStack exceptionStack = null;
            try {
                MultiException.checkAndThrow("Could not remove all devices!", removeExceptionStack);
            } catch (CouldNotPerformException ex) {
                exceptionStack = MultiException.push(this, ex, exceptionStack);
            }
            try {
                MultiException.checkAndThrow("Could not update all devices!", updateExceptionStack);
            } catch (CouldNotPerformException ex) {
                exceptionStack = MultiException.push(this, ex, exceptionStack);
            }
            try {
                MultiException.checkAndThrow("Could not register all devices!", registerExceptionStack);
            } catch (CouldNotPerformException ex) {
                exceptionStack = MultiException.push(this, ex, exceptionStack);
            }
            MultiException.checkAndThrow("Could not sync all devices!", exceptionStack);

        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Device registry sync failed!", ex);
        }
    }

    private boolean verifyDeviceConfig(final DeviceConfig config) throws CouldNotPerformException {
        try {

            // load device class
            DeviceClass deviceClass;
            try {
                deviceClass = remoteRegistry.getDeviceClassById(config.getDeviceClassId());
            } catch (CouldNotPerformException ex) {
                throw new CouldNotPerformException("Could not load device class of Device[" + config.getId() + "] !", ex);
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
            throw new CouldNotPerformException("Could not verify device config!", ex);
        }
    }
}
