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
import java.awt.Color;
import org.openbase.bco.dal.remote.unit.ColorableLightRemote;
import org.openbase.bco.dal.remote.unit.Units;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jps.core.JPService;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.domotic.state.PowerStateType.PowerState;

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
public class HowToControlAColorableLightUnitViaDAL {

    private static final Logger LOGGER = LoggerFactory.getLogger(HowToControlAColorableLightUnitViaDAL.class);

    public static void howto() throws InterruptedException {

        final ColorableLightRemote testLight;
        try {
            
            LOGGER.info("wait for registry connection...");
            Registries.waitForData();

            LOGGER.info("request the light unit with the label \"TestUnit_0\"");
            testLight = Units.getUnitByLabel("TestUnit_0", true, Units.LIGHT_COLORABLE);

            LOGGER.info("switch the light on");
            testLight.setPowerState(PowerState.State.ON);

            LOGGER.info("switch light color to blue");
            testLight.setColor(Color.BLUE);

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
