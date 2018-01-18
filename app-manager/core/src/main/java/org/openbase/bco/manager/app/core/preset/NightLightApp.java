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
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.pattern.Observer;
import org.openbase.jul.schedule.RecurrenceEventFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.domotic.state.PowerStateType.PowerState.State;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import rst.domotic.unit.location.LocationConfigType.LocationConfig.LocationType;
import rst.domotic.unit.location.LocationDataType.LocationData;
import rst.vision.HSBColorType.HSBColor;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * UnitConfig
 */
public class NightLightApp extends AbstractAppController {

    private static final Logger LOGGER = LoggerFactory.getLogger(NightLightApp.class);

    private Map<LocationRemote, Observer<LocationData>> locationMap;

    public NightLightApp() throws InstantiationException, InterruptedException {
        super(NightLightApp.class);
        try {
            this.locationMap = new HashMap<>();
            // init tile remotes
            for (final UnitConfig locationUnitConfig : Registries.getLocationRegistry(true).getLocationConfigsByType(LocationType.TILE)) {
                final LocationRemote remote = Units.getUnit(locationUnitConfig, false, Units.LOCATION);
                RecurrenceEventFilter<Void> eventFilter = new RecurrenceEventFilter<Void>() {
                    @Override
                    public void relay() throws Exception {
                        NightLightApp.this.update(remote);
                    }
                };
                locationMap.put(remote, (source, data) -> eventFilter.trigger());
            }
        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    @Override
    public void shutdown() {
        stop();
        locationMap.clear();
        super.shutdown();
    }

    @Override
    protected void execute() throws CouldNotPerformException, InterruptedException {
        locationMap.forEach((remote, observer) -> {
            remote.addDataObserver(observer);
            update(remote);
        });
    }

    @Override
    protected void stop() {
        locationMap.forEach((remote, observer) -> {
            remote.removeDataObserver(observer);
        });
    }

    public static final HSBColor COLOR_ORANGE = HSBColor.newBuilder().setHue(30).setSaturation(100).setBrightness(20).build();

    public void update(final LocationRemote location) {
        try {
            System.out.println("update: " + location.getLabel());
            switch (location.getMotionState().getValue()) {
                case MOTION:
                    if (!location.getColor().getHsbColor().equals(COLOR_ORANGE)) {
                        location.setColor(COLOR_ORANGE);
                    }
                    break;
                case NO_MOTION:
                    if (location.getPowerState(UnitType.LIGHT).getValue() == State.ON) {
                        location.setPowerState(State.OFF, UnitType.LIGHT);
                    }
                    break;
            }
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory("Could not switch light in night mode!", ex, LOGGER);
        }
    }
}
