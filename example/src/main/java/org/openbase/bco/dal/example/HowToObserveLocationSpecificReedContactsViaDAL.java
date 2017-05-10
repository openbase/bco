package org.openbase.bco.dal.example;

/*-
 * #%L
 * BCO DAL Example
 * %%
 * Copyright (C) 2014 - 2017 openbase.org
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
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jps.core.JPService;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

/**
 *
 * This howto shows how to observe reed contact units at one specific location.
 *
 * Note: This howto requires a running bco platform provided by your network.
 * Note: Please avoid hardcoding location \"scopes\" and \"labels\" because those can be dynamically changed by the end user even during runtime.
 * Note: The command-line tool \"bco-query Location\" will help you to get a list of available locations and there ids in your setup.
 *
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class HowToObserveLocationSpecificReedContactsViaDAL {

    private static final Logger LOGGER = LoggerFactory.getLogger(HowToObserveLocationSpecificReedContactsViaDAL.class);

    public static void howto() throws InterruptedException {

        try {
            LOGGER.info("wait for registry connection...");
            Registries.waitForData();

            // choose your location where the reed contacts are placed in.
            final String locationId = Registries.getLocationRegistry().getRootLocationConfig().getId();

            LOGGER.info("register observer on all reed contacts...");
            for (UnitConfig reedContactUnitConfig : Registries.getLocationRegistry().getUnitConfigsByLocation(UnitType.REED_CONTACT, locationId)) {
                Units.getUnit(reedContactUnitConfig, false, Units.REED_CONTACT).addDataObserver((source, data) -> {
                    LOGGER.info(source + " changed to " + data.getContactState().getValue().name());
                });
            }
            LOGGER.info("receiving state changes for one minute...");
            Thread.sleep(60000);
        } catch (CouldNotPerformException ex) {
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
