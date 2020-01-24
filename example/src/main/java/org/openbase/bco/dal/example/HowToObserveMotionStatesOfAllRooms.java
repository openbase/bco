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

import com.google.protobuf.Message;
import org.openbase.bco.dal.lib.layer.unit.UnitRemote;
import org.openbase.bco.dal.remote.layer.unit.CustomUnitPool;
import org.openbase.bco.dal.remote.layer.unit.Units;
import org.openbase.bco.dal.remote.layer.unit.location.LocationRemote;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.bco.registry.remote.login.BCOLogin;
import org.openbase.jps.core.JPService;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.extension.type.processing.LabelProcessor;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.service.ServiceTempusTypeType.ServiceTempusType.ServiceTempus;
import org.openbase.type.domotic.state.MotionStateType.MotionState;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import org.openbase.type.domotic.unit.location.LocationConfigType.LocationConfig.LocationType;
import org.openbase.type.domotic.unit.location.LocationDataType.LocationData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;


/**
 *
 * This howto demonstrates how all rooms of your smart environment can be observed regarding their current motion state.
 * BCO offers two different approaches that are both addressed within this howto by EXAMPLE 1 and EXAMPLE 2
 *
 * Note: You can use the PRESENCE_STATE service as well to rely not only on motion but also on other sensor events to detect a human presence at locations.
 * Note: This howto requires a running bco platform provided by your network.
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class HowToObserveMotionStatesOfAllRooms {

    private static final Logger LOGGER = LoggerFactory.getLogger(HowToObserveMotionStatesOfAllRooms.class);

    public static void howto() throws InterruptedException {

        try {
            LOGGER.info("wait for registry connection...");
            Registries.waitForData();

            LOGGER.info("authenticate current session...");
            BCOLogin.getSession().loginUserViaUsername("admin", "admin", false);

        // EXAMPLE 1: observe the movement in all rooms via a custom unit pool.

            // create a new unit pool which is mainly a collection of unit remotes
            final CustomUnitPool locationPool = new CustomUnitPool();

            // make sure the pool only contains tile locations (rooms are represented as tiles in bco).
            // so we want to filter all non locations and non tiles.
            locationPool.init(
                    unitConfig -> unitConfig.getUnitType() != UnitType.LOCATION,
                    unitConfig -> unitConfig.getLocationConfig().getLocationType() != LocationType.TILE
                    );

            // activate the pool so units get synchronized...
            locationPool.activate();

            // register an observer on each location and log when the movement state has changed.
            locationPool.addObserver((source, data) -> {

                // filter non motion state events
                if(source.getServiceType() != ServiceType.MOTION_STATE_SERVICE) {
                    return;
                }

                // we know the service providers are locations and we let only motion data pass because of the previous filter operation.
                final MotionState motionState = (MotionState) data;
                final LocationRemote locationRemote = (LocationRemote) source.getServiceProvider();

                // inform about the update. The location unit is delivered via the service provider method.
                LOGGER.info("EXAMPLE 1: "+LabelProcessor.getBestMatch(locationRemote.getConfig().getLabel(), "?") + " has changed its motion state to " + motionState.getValue().name());
            });

            // print a summary about the current movement state
            for (UnitRemote<? extends Message> unitRemote : locationPool.getInternalUnitList()) {
                // we know its a location remote
                final LocationRemote location = (LocationRemote) unitRemote;

                // we need to wait for the remote synchronisation when accessing any unit state at the first time
                location.waitForData();
                LOGGER.info("EXAMPLE 1: "+location.getLabel("?") + " has currently "+ location.getMotionState().getValue().name());
            }

        // EXAMPLE 2: observe the movement in all rooms via location remotes

            // query all tiles via the registry (rooms are represented as tiles in bco).
            final List<UnitConfig> locationConfigs = Registries.getUnitRegistry().getLocationUnitConfigsByLocationType(LocationType.TILE);

            // create remotes for all the locations
            final ArrayList<LocationRemote> locations = new ArrayList<>();
            for (UnitConfig locationConfig : locationConfigs) {
                locations.add(Units.getUnit(locationConfig, false, Units.LOCATION));
            }

            // register an observer on each location and log when the movement state has changed.
            for (LocationRemote location : locations) {
                location.addServiceStateObserver(ServiceTempus.CURRENT, ServiceType.MOTION_STATE_SERVICE, (source, data) -> {
                    // we know its a motion state
                    final MotionState motionState = (MotionState) data;
                    LOGGER.info("EXAMPLE 2: "+location.getLabel("?") + " has changed its motion state to " + motionState.getValue().name());
                });
            }

            // print a summary about the current movement state
            for (LocationRemote location : locations) {
                // we need to wait for the remote synchronisation when accessing any unit state at the first time
                location.waitForData();
                LOGGER.info("EXAMPLE 2: "+location.getLabel("?") + " has currently "+ location.getMotionState().getValue().name());
            }

            LOGGER.info("Observe changes for 2 minutes");
            Thread.sleep(120000);

        } catch (CouldNotPerformException | CancellationException ex) {
            ExceptionPrinter.printHistory(ex, LOGGER);
        }
    }

    public static void main(String[] args) throws InterruptedException {

        // Setup CLParser
        JPService.setApplicationName("howto");
        JPService.parseAndExitOnError(args);

        // Start HowTo
        LOGGER.info("start " + JPService.getApplicationName());
        howto();
        LOGGER.info("finish " + JPService.getApplicationName());
        System.exit(0);
    }
}
