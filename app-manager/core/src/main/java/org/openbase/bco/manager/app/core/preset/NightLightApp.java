package org.openbase.bco.manager.app.core.preset;

/*
 * #%L
 * BCO Manager App Core
 * %%
 * Copyright (C) 2015 - 2018 openbase.org
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

import org.openbase.bco.dal.remote.unit.Units;
import org.openbase.bco.dal.remote.unit.location.LocationRemote;
import org.openbase.bco.manager.app.core.AbstractAppController;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.pattern.Observer;
import org.openbase.jul.schedule.RecurrenceEventFilter;
import org.openbase.jul.schedule.SyncObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.domotic.state.ActivationStateType.ActivationState;
import rst.domotic.state.PowerStateType.PowerState.State;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import rst.domotic.unit.location.LocationConfigType.LocationConfig.LocationType;
import rst.domotic.unit.location.LocationDataType.LocationData;
import rst.vision.HSBColorType.HSBColor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * UnitConfig
 */
public class NightLightApp extends AbstractAppController {

    public static final HSBColor COLOR_ORANGE = HSBColor.newBuilder().setHue(30).setSaturation(100).setBrightness(20).build();
    private static final Logger LOGGER = LoggerFactory.getLogger(NightLightApp.class);
    private static final String META_CONFIG_KEY_EXCLUDE_LOCATION = "EXCLUDE_LOCATION";

    private SyncObject locationMapLock = new SyncObject("LocationMapLock");

    private Map<LocationRemote, Observer<LocationData>> locationMap;

    public NightLightApp() throws InstantiationException, InterruptedException {
        super(NightLightApp.class);
//        try {
        this.locationMap = new HashMap<>();
//        } catch (CouldNotPerformException ex) {
//            throw new InstantiationException(this, ex);
//        }
    }

    public static void update(final LocationRemote location) {
        try {
            System.out.println("update: " + location.getLabel());
            switch (location.getPresenceState().getValue()) {
                case PRESENT:
                    if (!location.getColor().getHsbColor().equals(COLOR_ORANGE)) {
                        System.out.println("Nightmode: switch orange " + location.getLabel() + " because of present state.");
                        location.setColor(COLOR_ORANGE);
                    }
                    break;
                case ABSENT:
                    if (location.getPowerState(UnitType.LIGHT).getValue() == State.ON) {
                        System.out.println("Nightmode: switch off " + location.getLabel() + " because of absent state.");
                        location.setPowerState(State.OFF, UnitType.LIGHT);
                    }
                    break;
            }
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory("Could not switch light in night mode!", ex, LOGGER);
        }
    }

    @Override
    public UnitConfig applyConfigUpdate(UnitConfig config) throws CouldNotPerformException, InterruptedException {
        final UnitConfig unitConfig = super.applyConfigUpdate(config);
        updateLocationMap();
        return unitConfig;
    }

    private void updateLocationMap() throws CouldNotPerformException {
        try {
            synchronized (locationMapLock) {

                final Collection<String> excludedLocations = new ArrayList<>();

                // check location exclusion
                try {
                    excludedLocations.addAll(generateVariablePool().getValues(META_CONFIG_KEY_EXCLUDE_LOCATION).values());
                } catch (final NotAvailableException ex) {
                    ExceptionPrinter.printHistory("Could not load variable pool!", ex, LOGGER);
                    // no locations excluded.
                }

                // deregister tile remotes
                locationMap.forEach((remote, observer) -> {
                    remote.removeDataObserver(observer);
                });

                // clear tile remotes
                locationMap.clear();

                System.out.println("load locations:");
                // load tile remotes
                remoteLocationLoop:
                for (final UnitConfig locationUnitConfig : Registries.getLocationRegistry(true).getLocationConfigsByType(LocationType.TILE)) {

                    // check if location was excluded
                    if (excludedLocations.contains(locationUnitConfig.getId())) {
                        System.out.println("exclude locations: " + locationUnitConfig.getLabel());
                        continue remoteLocationLoop;
                    }
                    for (String alias : locationUnitConfig.getAliasList()) {
                        if (excludedLocations.contains(alias)) {
                            System.out.println("exclude locations: " + locationUnitConfig.getLabel());
                            continue remoteLocationLoop;
                        }
                    }

                    final LocationRemote remote = Units.getUnit(locationUnitConfig, false, Units.LOCATION);

                    final RecurrenceEventFilter<Void> eventFilter = new RecurrenceEventFilter<Void>(10000) {
                        @Override
                        public void relay() throws Exception {
                            update(remote);
                        }
                    };
                    System.out.println("create observer for locations: " + locationUnitConfig.getLabel());
                    if (locationMap.containsKey(remote)) {
                        System.out.println("location " + remote.getLabel() + "already registered!");
                    }
                    locationMap.put(remote, (source, data) -> eventFilter.trigger());
                }

                if (getActivationState().getValue() == ActivationState.State.ACTIVE) {
                    locationMap.forEach((remote, observer) -> {
                        remote.addDataObserver(observer);
                        update(remote);
                    });
                }
            }
        } catch (final InterruptedException ex) {
            //todo remove me later
        } catch (final CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not update location map", ex);
        }
    }

    @Override
    public void shutdown() {
        stop();
        synchronized (locationMapLock) {
            locationMap.clear();
        }
        super.shutdown();
    }

    @Override
    protected void execute() throws CouldNotPerformException, InterruptedException {
        synchronized (locationMapLock) {
            locationMap.forEach((remote, observer) -> {
                remote.addDataObserver(observer);
                update(remote);
            });
        }
    }

    @Override
    protected void stop() {
        synchronized (locationMapLock) {
            locationMap.forEach((remote, observer) -> {
                remote.removeDataObserver(observer);
            });
        }
    }
}
