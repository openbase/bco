package org.openbase.bco.device.openhab.manager;

/*-
 * #%L
 * BCO Openhab Device Manager
 * %%
 * Copyright (C) 2015 - 2020 openbase.org
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

import org.eclipse.smarthome.io.rest.core.item.EnrichedItemDTO;
import org.openbase.bco.dal.control.layer.unit.device.DeviceManagerImpl;
import org.openbase.bco.device.openhab.communication.OpenHABRestCommunicator;
import org.openbase.bco.device.openhab.manager.service.OpenHABOperationServiceFactory;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.ExceptionProcessor;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.LogLevel;
import org.openbase.jul.iface.Launchable;
import org.openbase.jul.iface.VoidInitializable;
import org.openbase.jul.pattern.Observer;
import org.openbase.jul.schedule.RecurrenceEventFilter;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.domotic.unit.device.DeviceClassType.DeviceClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenHABDeviceManager extends DeviceManagerImpl implements Launchable<Void>, VoidInitializable {

    public static final String ITEM_STATE_TOPIC_FILTER = "smarthome/items/(.+)/state";

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenHABDeviceManager.class);

    private final CommandExecutor commandExecutor;
    /**
     * Synchronization observer that triggers resynchronization of all units if their configuration changes.
     */
    private final Observer synchronizationObserver;
    private final RecurrenceEventFilter<Object> unitChangeSynchronizationFilter;

    public OpenHABDeviceManager() throws InterruptedException, InstantiationException {
        super(new OpenHABOperationServiceFactory() ,false);

        // the sync observer triggers a lot when the device manager is initially activated and all unit controllers are created
        // thus add an event filter
        this.unitChangeSynchronizationFilter = new RecurrenceEventFilter<Object>(5000) {
            @Override
            public void relay() {
                try {
                    for (final EnrichedItemDTO item : OpenHABRestCommunicator.getInstance().getItems()) {
                        try {
                            commandExecutor.applyStateUpdate(item.name, item.type, item.state, true);
                        } catch (CouldNotPerformException ex) {
                            ExceptionPrinter.printHistory("Skip synchronization of item[name:" + item.name + ", label:" + item.label + ", type:" + item.type + ", " + ", state:" + item.state + ", " + item.stateDescription + ", transformedState:" + item.transformedState + ", category:" + item.category+  "]", ex, LOGGER, LogLevel.WARN);
                        }
                    }
                } catch (CouldNotPerformException ex) {
                    if (!ExceptionProcessor.isCausedBySystemShutdown(ex)) {
                        ExceptionPrinter.printHistory("Could not retrieve item states from openHAB!", ex, LOGGER, LogLevel.WARN);
                    }
                }
            }
        };
        this.commandExecutor = new CommandExecutor(getUnitControllerRegistry());
        this.synchronizationObserver = ((observable, value) -> unitChangeSynchronizationFilter.trigger());
    }

    @Override
    public boolean isSupported(UnitConfig config) {
        DeviceClass deviceClass;
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

    @Override
    public void activate() throws CouldNotPerformException, InterruptedException {
        getUnitControllerRegistry().addObserver(synchronizationObserver);
        super.activate();
        OpenHABRestCommunicator.getInstance().addSSEObserver(commandExecutor, ITEM_STATE_TOPIC_FILTER);
        unitChangeSynchronizationFilter.trigger();
    }

    @Override
    public void deactivate() throws CouldNotPerformException, InterruptedException {
        getUnitControllerRegistry().removeObserver(synchronizationObserver);
        OpenHABRestCommunicator.getInstance().removeSSEObserver(commandExecutor, ITEM_STATE_TOPIC_FILTER);
        super.deactivate();
    }
}
