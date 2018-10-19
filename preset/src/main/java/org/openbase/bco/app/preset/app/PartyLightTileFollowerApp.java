package org.openbase.bco.app.preset.app;

/*
 * #%L
 * BCO App Preset
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

import org.openbase.bco.dal.remote.layer.unit.Units;
import org.openbase.bco.dal.remote.layer.unit.location.LocationRemote;
import org.openbase.bco.dal.control.layer.unit.app.AbstractAppController;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.InvalidStateException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.schedule.SyncObject;
import rst.domotic.state.ActivationStateType.ActivationState;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitTemplateType;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import rst.domotic.unit.location.LocationConfigType;
import rst.vision.HSBColorType.HSBColor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * UnitConfig
 */
public class PartyLightTileFollowerApp extends AbstractAppController {

    private Map<String, LocationRemote> locationRemoteMap;
    private SyncObject taskLock = new SyncObject("TaskLock");

    public PartyLightTileFollowerApp() throws InstantiationException, InterruptedException {
        super(PartyLightTileFollowerApp.class);
        try {
            Registries.waitForData();
            Registries.waitForData();
            this.locationRemoteMap = new HashMap<>();

            // init tile remotes
            for (final UnitConfig locationUnitConfig : Registries.getUnitRegistry().getUnitConfigs(UnitType.LOCATION)) {
                if (!locationUnitConfig.getLocationConfig().getType().equals(LocationConfigType.LocationConfig.LocationType.TILE)) {
                    continue;
                }
                locationRemoteMap.put(locationUnitConfig.getId(), Units.getUnit(locationUnitConfig, false, Units.LOCATION));
            }
        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    @Override
    public void shutdown() {
        locationRemoteMap.clear();
        super.shutdown();
    }

    private double brightness = 50;
    private HSBColor[] colors = {
            HSBColor.newBuilder().setHue(0).setSaturation(100).setBrightness(brightness).build(),
            HSBColor.newBuilder().setHue(290).setSaturation(100).setBrightness(brightness).build(),
            HSBColor.newBuilder().setHue(30).setSaturation(100).setBrightness(brightness).build(),};

    @Override
    protected void execute(final ActivationState activationState) throws CouldNotPerformException, InterruptedException {
        logger.debug("Execute PartyLightTileFollowerApp[" + getLabel() + "]");
        // verify
        if (!Registries.getUnitRegistry().getUnitConfigById(getConfig().getPlacementConfig().getLocationId()).getLocationConfig().getType().equals(LocationConfigType.LocationConfig.LocationType.TILE)) {
            throw new InvalidStateException("App location is not a tile!");
        }

        new TileFollower().call();
    }

    @Override
    protected void stop(final ActivationState activationState) {
    }

    public class TileFollower implements Callable<Void> {

        private final List<String> processedLocations = new ArrayList<>();

        @Override
        public Void call() throws CouldNotPerformException, InterruptedException {
            if (locationRemoteMap.isEmpty()) {
                throw new CouldNotPerformException("No Locations found!");
            }

            LocationRemote locationRemote;

            int colorIndex = 0;
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    // apply updates for next iteration
                    colorIndex = ++colorIndex % colors.length;
                    processedLocations.clear();

                    // select initial room
                    locationRemote = locationRemoteMap.get(getConfig().getPlacementConfig().getLocationId());

                    processRoom(locationRemote, colors[colorIndex]);

                } catch (CouldNotPerformException ex) {
                    ExceptionPrinter.printHistory(new CouldNotPerformException("Skip animation run!", ex), logger);
                }
            }

            return null;
        }

        private void processRoom(final LocationRemote locationRemote, final HSBColor color) throws CouldNotPerformException, InterruptedException {
            logger.debug("Set " + locationRemote + " to " + color + "...");
            try {

                // skip if no colorable light is present
                if (!Registries.getUnitRegistry().getUnitConfigsByLocation(UnitTemplateType.UnitTemplate.UnitType.COLORABLE_LIGHT, locationRemote.getId()).isEmpty()) {
                    try {
                        if (locationRemote.isConnected() && locationRemote.isDataAvailable()) {
                            locationRemote.setColor(color).get(5, TimeUnit.SECONDS);
                        }
                    } catch (TimeoutException ex) {
                        ExceptionPrinter.printHistory(new CouldNotPerformException("Could not set color!", ex), logger);
                    }
                    Thread.sleep(2000);
                }

                // mark as processed
                processedLocations.add(locationRemote.getId());

                // process neighbors
                LocationRemote neighborRemote;
                for (UnitConfig neighborConfig : Registries.getUnitRegistry().getNeighborLocations(locationRemote.getId())) {
                    // skip if already processed
                    if (processedLocations.contains(neighborConfig.getId())) {
                        continue;
                    }

                    neighborRemote = locationRemoteMap.get(neighborConfig.getId());

                    // process remote 
                    processRoom(neighborRemote, color);
                }
            } catch (CouldNotPerformException | ExecutionException ex) {
                throw new CouldNotPerformException("Could not process room of " + locationRemote, ex);
            }
        }
    }
}
