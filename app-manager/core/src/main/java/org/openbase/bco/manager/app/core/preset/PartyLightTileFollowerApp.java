package org.openbase.bco.manager.app.core.preset;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.openbase.bco.manager.app.core.AbstractApp;
import org.openbase.bco.dal.remote.unit.location.LocationRemote;
import org.openbase.bco.registry.location.lib.LocationRegistry;
import org.openbase.bco.registry.location.remote.CachedLocationRegistryRemote;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.InvalidStateException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.schedule.GlobalExecutionService;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitTemplateType;
import rst.domotic.unit.location.LocationConfigType;
import rst.vision.HSBColorType.HSBColor;

/*
 * #%L
 * COMA AppManager Core
 * %%
 * Copyright (C) 2015 - 2016 openbase.org
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
/**
 * UnitConfig
 */
public class PartyLightTileFollowerApp extends AbstractApp {

    private Map<String, LocationRemote> locationRemoteMap;
    private LocationRegistry locationRegistry;

    public PartyLightTileFollowerApp() throws InstantiationException, InterruptedException {
        super(true);
        try {
            CachedLocationRegistryRemote.waitForData();
            this.locationRegistry = CachedLocationRegistryRemote.getRegistry();
            this.locationRemoteMap = new HashMap<>();

            LocationRemote locationRemote;
            // init tile remotes
            for (UnitConfig locationUnitConfig : locationRegistry.getLocationConfigs()) {
                if (!locationUnitConfig.getLocationConfig().getType().equals(LocationConfigType.LocationConfig.LocationType.TILE)) {
                    continue;
                }
                locationRemote = new LocationRemote();
                locationRemoteMap.put(locationUnitConfig.getId(), locationRemote);
                locationRemote.init(locationUnitConfig);
                locationRemote.activate();
            }

        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    @Override
    public void shutdown() {
        // shutdown tile remotes
        locationRemoteMap.values().stream().forEach((locationRemote) -> {
            locationRemote.shutdown();
        });
        super.shutdown();
    }

    private double brightness = 50;
    private HSBColor[] colors = {
        HSBColor.newBuilder().setHue(0).setSaturation(100).setBrightness(brightness).build(),
        HSBColor.newBuilder().setHue(290).setSaturation(100).setBrightness(brightness).build(),
        HSBColor.newBuilder().setHue(30).setSaturation(100).setBrightness(brightness).build(),};

    private Future<Void> tileFollowerFuture;

    @Override
    protected void execute() throws CouldNotPerformException, InterruptedException {

        // verify
        if (!locationRegistry.getLocationConfigById(getConfig().getId()).getLocationConfig().getType().equals(LocationConfigType.LocationConfig.LocationType.TILE)) {
            throw new InvalidStateException("App location is not a tile!");
        }

        // execute
        if (tileFollowerFuture != null) {
            logger.warn(this + " is already executing!");
            return;
        }
        tileFollowerFuture = GlobalExecutionService.submit(new TileFollower());

    }

    @Override
    protected void stop() throws CouldNotPerformException, InterruptedException {
        if (tileFollowerFuture != null) {
            tileFollowerFuture.cancel(true);
            tileFollowerFuture = null;
        }
    }

    public class TileFollower implements Callable<Void> {

        private List<String> processedLocations = new ArrayList<>();

        @Override
        public Void call() throws CouldNotPerformException, InterruptedException {
            logger.info("Execute " + this);
            if (locationRemoteMap.isEmpty()) {
                throw new CouldNotPerformException("No Locations found!");
            }

            LocationRemote locationRemote;

            int colorIndex = 0;
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    // apply updates for next iteration
                    colorIndex = ++colorIndex % colors.length;
                    System.out.println("color index:" + colorIndex);
                    processedLocations.clear();

                    // select inital room
                    locationRemote = locationRemoteMap.get(getConfig().getId());

                    processRoom(locationRemote, colors[colorIndex]);

                } catch (CouldNotPerformException ex) {
                    ExceptionPrinter.printHistory(new CouldNotPerformException("Skip animation run!", ex), logger);
                }

                logger.info("#########################");

            }
            return null;
        }

        public void processRoom(final LocationRemote locationRemote, final HSBColor color) throws CouldNotPerformException, InterruptedException {
            logger.info("Set " + locationRemote + " to " + color + "...");
            try {

                // skip if no ambi light is present
                if (!locationRegistry.getUnitConfigsByLocation(UnitTemplateType.UnitTemplate.UnitType.COLORABLE_LIGHT, locationRemote.getId()).isEmpty()) {
                    try {
                        locationRemote.setColor(color).get(1, TimeUnit.SECONDS);
                    } catch (TimeoutException ex) {
                        ExceptionPrinter.printHistory(new CouldNotPerformException("Could not set color!", ex), logger);
                    }
                    Thread.sleep(500);
                }

                // mark as prcessed        
                processedLocations.add(locationRemote.getId());

                // process neighbors
                LocationRemote neighborRemote;
                for (String neighborsId : locationRemote.getNeighborLocationIds()) {
                    // skip if already processed
                    if (processedLocations.contains(neighborsId)) {
                        continue;
                    }

                    neighborRemote = locationRemoteMap.get(neighborsId);

                    // process remote 
                    processRoom(neighborRemote, color);
                }
            } catch (CouldNotPerformException | ExecutionException ex) {
                throw new CouldNotPerformException("Could not process room of " + locationRemote);
            }
        }
    }
}
