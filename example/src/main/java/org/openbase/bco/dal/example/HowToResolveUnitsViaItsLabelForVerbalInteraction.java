package org.openbase.bco.dal.example;

/*-
 * #%L
 * BCO DAL Example
 * %%
 * Copyright (C) 2014 - 2020 openbase.org
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

import org.openbase.bco.dal.remote.layer.service.PowerStateServiceRemote;
import org.openbase.bco.dal.remote.layer.unit.Units;
import org.openbase.bco.dal.remote.layer.unit.location.LocationRemote;
import org.openbase.bco.registry.lib.util.UnitConfigProcessor;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.bco.registry.remote.login.BCOLogin;
import org.openbase.jps.core.JPService;
import org.openbase.jps.preset.JPVerbose;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.extension.type.processing.LabelProcessor;
import org.openbase.type.domotic.state.MotionStateType.MotionState.State;
import org.openbase.type.domotic.state.PowerStateType.PowerState;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


/**
 * This howto demonstrates how a unit can be resolved via its label.
 * <p>
 * Note: Labels should never be hardcoded because they can change during runtime.
 * In most cases, a generic implementation avoids hardlinks by using the registry and accessing units via its location.
 * In case you are writing a prototype, please use only aliases or unit ids to refer to any units within your code.
 * <p>
 * Note: This howto requires a running bco platform provided by your network.
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class HowToResolveUnitsViaItsLabelForVerbalInteraction {

    private static final Logger LOGGER = LoggerFactory.getLogger(HowToResolveUnitsViaItsLabelForVerbalInteraction.class);

    public static void howto() throws InterruptedException {

        try {
            LOGGER.info("wait for registry connection...");
            Registries.waitForData();

            LOGGER.info("authenticate current session...");
            BCOLogin.getSession().loginUserViaUsername("admin", "admin", false);

            // When working with verbal interaction or dialog systems, we sometimes need to resolve units via its label to access their services.
            // Let us checkout the following example where the speech processing pipeline already identified the label of the target unit to control and the desired power state.
            String unitLabel = "Ceiling Lamp 2";
            String powerState = "ON";

            // Now we need to lookup the target unit via its label.
            final List<UnitConfig> targetUnitConfigs = Registries.getUnitRegistry().getUnitConfigsByLabel(unitLabel);

            // Labels are not a unique identifier of a unit, since multiple units with the same label can be placed at different locations.
            // Therefore, one strategy to identify the right one could be to filter all locations without movement:
            for (UnitConfig targetUnitConfig : targetUnitConfigs) {

                // filter all base units (scene, location, app, agent, device, etc.) because they do not support the
                // power state operation service anyway but could possibly be queried by the label as well.
                if (UnitConfigProcessor.isBaseUnit(targetUnitConfig)) {
                    continue;
                }

                // lookup the location of the unit
                final LocationRemote location = Units.getUnit(targetUnitConfig.getPlacementConfig().getLocationId(), true, Units.LOCATION);

                // filter all locations without movement
                if (location.getMotionState().getValue() != State.MOTION) {
                    continue;
                }

                // control the target unit
                try {
                    // because we just want to control the power state independent of the actual unit type, we can use a service remote and let bco do the mapping.
                    PowerStateServiceRemote powerStateServiceRemote = new PowerStateServiceRemote();
                    powerStateServiceRemote.init(targetUnitConfig);

                    // set the power state of the target unit
                    LOGGER.info("Set power state of " + LabelProcessor.getBestMatch(targetUnitConfig.getLabel(), "?") + " to " + powerState);
                    powerStateServiceRemote.setPowerState(PowerState.State.valueOf(powerState)).get(5, TimeUnit.SECONDS);
                } catch (CouldNotPerformException | ExecutionException | TimeoutException ex) {
                    ExceptionPrinter.printHistory("Could not control " + LabelProcessor.getBestMatch(targetUnitConfig.getLabel(), "?"), ex, LOGGER);
                }
            }

        } catch (CouldNotPerformException | CancellationException ex) {
            ExceptionPrinter.printHistory(ex, LOGGER);
        }
    }

    public static void main(String[] args) throws InterruptedException {

        // Setup CLParser
        JPService.setApplicationName("howto");
        JPService.registerProperty(JPVerbose.class, true);
        JPService.parseAndExitOnError(args);


        // Start HowTo
        LOGGER.info("start " + JPService.getApplicationName());
        howto();
        LOGGER.info("finish " + JPService.getApplicationName());
        System.exit(0);
    }
}
