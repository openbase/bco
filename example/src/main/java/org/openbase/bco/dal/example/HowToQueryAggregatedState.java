package org.openbase.bco.dal.example;

/*
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

import org.openbase.bco.dal.remote.layer.unit.Units;
import org.openbase.bco.dal.remote.layer.unit.location.LocationRemote;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.bco.registry.remote.login.BCOLogin;
import org.openbase.jps.core.JPService;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.type.domotic.database.QueryType;
import org.openbase.type.domotic.database.QueryType.Query;
import org.openbase.type.domotic.database.RecordCollectionType;
import org.openbase.type.domotic.service.ServiceTemplateType;
import org.openbase.type.domotic.state.AggregatedServiceStateType;
import org.openbase.type.domotic.state.AggregatedServiceStateType.AggregatedServiceState;
import org.openbase.type.timing.TimestampType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * This howto shows how to control a colorable light via the bco-dal-remote api.
 * <p>
 * Note: This howto requires a running bco platform provided by your network.
 * Note: If your setup does not provide a light unit called \"TestUnit_0"\ you
 * can use the command-line tool \"bco-query ColorableLight\" to get a list of available colorable lights
 * in your setup.
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class HowToQueryAggregatedState {

    private static final Logger LOGGER = LoggerFactory.getLogger(HowToQueryAggregatedState.class);

    public static void howto() throws InterruptedException {

        final LocationRemote testLocation;
        try {

            LOGGER.info("wait for registry connection...");
            Registries.waitForData();

            LOGGER.info("authenticate current session...");
            BCOLogin.getSession().loginUserViaUsername("admin", "admin", false);

            Date date = new Date();

            long time = TimeUnit.MILLISECONDS.toSeconds(date.getTime());

            testLocation = Units.getRootLocation(true);
            // Query for continuous data
            Query query = Query.newBuilder()
                    .setMeasurement("power_consumption_state_service")
                    .setServiceType(ServiceTemplateType.ServiceTemplate.ServiceType.POWER_CONSUMPTION_STATE_SERVICE)
                    .setTimeRangeStart(TimestampType.Timestamp.newBuilder().setTime(time - 3600).build())
                    .setTimeRangeStop(TimestampType.Timestamp.newBuilder().setTime(time).build())
                    .setAggregatedWindow("1m")
                    .build();

            // Query for enum data
            Query enumQuery = Query.newBuilder()
                    .setMeasurement("button_state_service")
                    .setServiceType(ServiceTemplateType.ServiceTemplate.ServiceType.BUTTON_STATE_SERVICE)
                    .setTimeRangeStart(TimestampType.Timestamp.newBuilder().setTime(time - 3600).build())
                    .setTimeRangeStop(TimestampType.Timestamp.newBuilder().setTime(time).build())
                    .setAggregatedWindow("1m")
                    .build();
            AggregatedServiceState aggregatedServiceState = testLocation.queryAggregatedServiceState(query).get();
            AggregatedServiceState aggregatedEnumServiceState = testLocation.queryAggregatedServiceState(enumQuery).get();

            LOGGER.info(aggregatedServiceState.toString());
            LOGGER.info(aggregatedEnumServiceState.toString());


        } catch (CouldNotPerformException | CancellationException | ExecutionException ex) {
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
