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

import org.openbase.bco.dal.remote.action.RemoteAction;
import org.openbase.bco.dal.remote.layer.unit.ColorableLightRemote;
import org.openbase.bco.dal.remote.layer.unit.Units;
import org.openbase.bco.dal.remote.layer.unit.location.LocationRemote;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.bco.registry.remote.login.BCOLogin;
import org.openbase.jps.core.JPService;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.type.domotic.action.ActionDescriptionType.ActionDescription;
import org.openbase.type.domotic.database.QueryType.Query;
import org.openbase.type.domotic.database.RecordCollectionType;
import org.openbase.type.domotic.service.ServiceTemplateType;
import org.openbase.type.domotic.state.PowerStateType.PowerState.State;
import org.openbase.type.vision.HSBColorType.HSBColor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 *
 * This howto shows how to control a colorable light via the bco-dal-remote api.
 *
 * Note: This howto requires a running bco platform provided by your network.
 * Note: If your setup does not provide a light unit called \"TestUnit_0"\ you
 * can use the command-line tool \"bco-query ColorableLight\" to get a list of available colorable lights
 * in your setup.
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class HowToQueryUnitLongTermStateUpdates {

    private static final Logger LOGGER = LoggerFactory.getLogger(HowToQueryUnitLongTermStateUpdates.class);

    public static void howto() throws InterruptedException {

        final LocationRemote testLocation;
        try {

            Date date = new Date();
            // Get current time in seconds
            long time = TimeUnit.MILLISECONDS.toSeconds(date.getTime());

            String query = "from(bucket: \"bco-persistence\")\n" +
                    "  |> range(start:" + (time - 3600) + ", stop: " + time + ")\n" +
                    "  |> filter(fn: (r) => r._measurement == \"power_consumption_state_service\")";

            LOGGER.info("wait for registry connection...");
            Registries.waitForData();

            LOGGER.info("authenticate current session...");
            BCOLogin.getSession().loginUserViaUsername("admin", "admin", false);

            LOGGER.info("request the root location");

            testLocation = Units.getRootLocation(true);

            RecordCollectionType.RecordCollection recordCollection = testLocation.queryRecord(Query.newBuilder().setRawQuery(query).build()).get();


            LOGGER.info(recordCollection.toString());

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
