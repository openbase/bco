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
import org.openbase.jul.schedule.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.state.ActivationStateType.ActivationState;
import rst.domotic.state.PowerStateType.PowerState.State;
import rst.domotic.state.PresenceStateType.PresenceState;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import rst.domotic.unit.location.LocationConfigType.LocationConfig.LocationType;
import rst.domotic.unit.location.LocationDataType.LocationData;
import rst.vision.HSBColorType.HSBColor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

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
        this.locationMap = new HashMap<>();
    }

    public static void update(final LocationRemote location, final Timeout timeout) {
        try {

            // init present state with main location.
            PresenceState.State presentState = location.getPresenceState().getValue();
            for (final LocationRemote neighbor : location.getNeighborLocationList(true)) {

                // break if any present person is detected.
                if (presentState == PresenceState.State.PRESENT) {
                    break;
                }

                // if not unknown apply state of neighbor
                if (neighbor.getPresenceState().getValue() != PresenceState.State.UNKNOWN) {
                    presentState = neighbor.getPresenceState().getValue();
                }
            }

            switch (presentState) {
                case PRESENT:
                    if (timeout != null) {
                        timeout.restart();
                    }
                    if (location.getPowerState(UnitType.COLORABLE_LIGHT).getValue() != State.ON || !isColorReached(location.getColor().getHsbColor(), COLOR_ORANGE)) {
                    // System.out.println("Nightmode: switch location " + location.getLabel() + " to orange because of present state].");
                        location.setColor(COLOR_ORANGE);
                    }
                    break;
                case ABSENT:
                    if (location.getPowerState(UnitType.LIGHT).getValue() == State.ON && (timeout == null || timeout.isExpired() || !timeout.isActive())) {
                        // System.out.println("Location power State[" + location.getPowerState().getValue() + ". " + location.getPowerState(UnitType.LIGHT).getValue() + "]");
                        // System.out.println("Nightmode: switch off " + location.getLabel() + " because of absent state.");
                        location.setPowerState(State.OFF, UnitType.LIGHT);
                    }
                    break;
            }
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory("Could not switch light in night mode!", ex, LOGGER);
        }
    }

    private static boolean isColorReached(final HSBColor hsbColorA, final HSBColor hsbColorB) {
        return withinMargin(hsbColorA.getHue(), hsbColorB.getHue(), 10)
                && withinMargin(hsbColorA.getSaturation(), hsbColorB.getSaturation(), 5)
                && withinMargin(hsbColorA.getBrightness(), hsbColorB.getBrightness(), 5);
    }

    private static boolean withinMargin(double a, double b, double margin) {
        return Math.abs(a - b) < margin;
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

                // load tile remotes
                remoteLocationLoop:
                for (final UnitConfig locationUnitConfig : Registries.getLocationRegistry(true).getLocationConfigsByType(LocationType.TILE)) {

                    // check if location was excluded
                    if (excludedLocations.contains(locationUnitConfig.getId())) {
                        // System.out.println("exclude locations: " + locationUnitConfig.getLabel());
                        continue remoteLocationLoop;
                    }
                    for (String alias : locationUnitConfig.getAliasList()) {
                        if (excludedLocations.contains(alias)) {
                            // System.out.println("exclude locations: " + locationUnitConfig.getLabel());
                            continue remoteLocationLoop;
                        }
                    }

                    final LocationRemote remote = Units.getUnit(locationUnitConfig, false, Units.LOCATION);

                    final Timeout nightModeTimeout;
                    nightModeTimeout = new Timeout(10, TimeUnit.MINUTES) {
                        @Override
                        public void expired() throws InterruptedException {
                            update(remote, this);
                        }
                    };


                    final RecurrenceEventFilter<Void> eventFilter = new RecurrenceEventFilter<Void>(1000) {
                        @Override
                        public void relay() throws Exception {
                            update(remote, nightModeTimeout);
                        }
                    };
                    locationMap.put(remote, (source, data) -> eventFilter.trigger());
                }

                if (getActivationState().getValue() == ActivationState.State.ACTIVE) {
                    locationMap.forEach((remote, observer) -> {
                        remote.addServiceStateObserver(ServiceType.PRESENCE_STATE_SERVICE, observer);
                        update(remote, null);
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
                try {
                    for (LocationRemote neighbor : remote.getNeighborLocationList(false)) {
                        neighbor.addDataObserver(observer);
                    }
                } catch (CouldNotPerformException ex) {
                    ExceptionPrinter.printHistory("Could not register observer on neighbor locations.", ex, LOGGER);
                }
                update(remote, null);
            });
        }
    }

    @Override
    protected void stop() {
        synchronized (locationMapLock) {
            locationMap.forEach((remote, observer) -> {
                remote.removeDataObserver(observer);
                try {
                    for (LocationRemote neighbor : remote.getNeighborLocationList(false)) {
                        neighbor.removeDataObserver(observer);
                    }
                } catch (CouldNotPerformException ex) {
                    ExceptionPrinter.printHistory("Could not remove observer from neighbor locations.", ex, LOGGER);
                }
            });
        }
    }
}
