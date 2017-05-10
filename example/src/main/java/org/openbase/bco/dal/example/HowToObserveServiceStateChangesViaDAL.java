package org.openbase.bco.dal.example;

/*
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
import org.openbase.jps.core.JPService;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.bco.registry.remote.Registries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.unit.UnitConfigType;

/**
 *
 * This howto shows how to observe service state changes of any units providing the given service.
 *
 * Note: This howto requires a running bco platform provided by your network.
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class HowToObserveServiceStateChangesViaDAL {

    private static final Logger LOGGER = LoggerFactory.getLogger(HowToObserveServiceStateChangesViaDAL.class);

    public static void howto() throws InterruptedException {

        try {
            // choose your service type to listen
            final ServiceType serviceType = ServiceType.COLOR_STATE_SERVICE;

            LOGGER.info("wait for registry connection...");
            Registries.waitForData();

            LOGGER.info("register observer on all Service[" + serviceType.name() + "] compatible units...");
            for (UnitConfigType.UnitConfig unitConfig : Registries.getUnitRegistry().getUnitConfigsByServices(serviceType)) {
                Units.getUnit(unitConfig, false).addDataObserver((source, data) -> {
                    LOGGER.info("Got Event[" + data + "] of " + source);
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
