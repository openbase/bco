package org.openbase.bco.app.openhab.manager;

/*-
 * #%L
 * BCO Openhab App
 * %%
 * Copyright (C) 2018 openbase.org
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

import org.openbase.bco.app.openhab.OpenHABRestCommunicator;
import org.openbase.bco.app.openhab.manager.service.OpenHABOperationServiceFactory;
import org.openbase.bco.manager.device.core.DeviceManagerImpl;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.iface.Launchable;
import org.openbase.jul.iface.VoidInitializable;
import org.openbase.jul.pattern.Observer;
import org.openbase.jul.schedule.RecurrenceEventFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import rst.domotic.unit.device.DeviceClassType.DeviceClass;

import java.util.Map.Entry;

public class OpenHABDeviceManager implements Launchable<Void>, VoidInitializable {

    public static final String ITEM_STATE_TOPIC_FILTER = "smarthome/items/(.+)/state";

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenHABDeviceManager.class);

    private final DeviceManagerImpl deviceManager;
    private final CommandExecutor commandExecutor;
    /**
     * Synchronization observer that triggers resynchronization of all units if their configuration changes.
     */
    private final Observer synchronizationObserver;

    public OpenHABDeviceManager() throws InterruptedException, InstantiationException {
        this.deviceManager = new DeviceManagerImpl(new OpenHABOperationServiceFactory()) {

            @Override
            public boolean isSupported(UnitConfig config) {
                DeviceClass deviceClass = null;
                try {
                    try {
                        deviceClass = Registries.getClassRegistry(true).getDeviceClassById(config.getDeviceConfig().getDeviceClassId());
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return false;
                    }
                } catch (CouldNotPerformException e) {
                    return false;
                }
                if (!deviceClass.getBindingConfig().getBindingId().equals("OPENHAB")) {
                    return false;
                }

                return super.isSupported(config);
            }
        };

        // the sync observer triggers a lot when the manager is initially activated ann all unit controller are created
        // thus add an event filter
        final RecurrenceEventFilter<Object> unitChangeSynchronizationFilter = new RecurrenceEventFilter<Object>(5000) {
            @Override
            public void relay() {
                try {
                    for (final Entry<String, String> entry : OpenHABRestCommunicator.getInstance().getStates().entrySet()) {
                        try {
                            commandExecutor.applyStateUpdate(entry.getKey(), entry.getValue());
                        } catch (CouldNotPerformException ex) {
                            LOGGER.warn("Skip synchronization of item[" + entry.getKey() + "] state[" + entry.getValue() + "] because unit not available", ex);
                        }
                    }
                } catch (CouldNotPerformException e) {
                    LOGGER.error("Could not retrieve item states from openHAB");
                }
            }
        };
        this.commandExecutor = new CommandExecutor(deviceManager.getUnitControllerRegistry());
        this.synchronizationObserver = ((observable, value) -> unitChangeSynchronizationFilter.trigger());
    }

    @Override
    public void init() throws InterruptedException, InitializationException {
        deviceManager.init();
    }

    @Override
    public void activate() throws CouldNotPerformException, InterruptedException {
        // TODO: this is a hack implemented because waitForData did not work correctly last time tested
        while (Registries.getUnitRegistry().getUnitConfigs(UnitType.USER).size() == 0) {
            Thread.sleep(100);
        }
        deviceManager.getUnitControllerRegistry().addObserver(synchronizationObserver);
        deviceManager.activate();
        OpenHABRestCommunicator.getInstance().addSSEObserver(commandExecutor, ITEM_STATE_TOPIC_FILTER);
    }

    @Override
    public void deactivate() throws CouldNotPerformException, InterruptedException {
        deviceManager.getUnitControllerRegistry().removeObserver(synchronizationObserver);
        OpenHABRestCommunicator.getInstance().removeSSEObserver(commandExecutor, ITEM_STATE_TOPIC_FILTER);
        deviceManager.deactivate();
    }

    @Override
    public boolean isActive() {
        return deviceManager.isActive();
    }
}
