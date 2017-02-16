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
import org.openbase.jul.exception.printer.LogLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rsb.Scope;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class ControlAmbientLightViaRemoteLib {

    private static final Logger logger = LoggerFactory.getLogger(ControlAmbientLightViaRemoteLib.class);

    public void notifyAlarm() throws CouldNotPerformException, InterruptedException {

        final ColorableLightRemote testLight;
        try {
            Registries.waitForData();
            Units.getUnit(Registries.getUnitRegistry().getUnitConfigById("59ca561e-a44d-406b-84ad-27a344cc2eb8"), false);
            testLight = (ColorableLightRemote) Units.getUnit("53b59c91-dd89-4a24-95ae-0ba841634039", false);
            System.out.println("got lamp");
            testLight.waitForData();
            System.out.println("git lamp data");
            int delay = 500;
            int rounds = 100;

            for (int i = 0; i < rounds; i++) {
                try {
                    testLight.setColor(Color.BLUE);
                    Thread.sleep(delay);
                    testLight.setColor(Color.RED);
                    Thread.sleep(delay);
                } catch (CouldNotPerformException ex) {
                    ExceptionPrinter.printHistory(new CouldNotPerformException("Could not change color!", ex), logger, LogLevel.ERROR);
                }
            }
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not notify alarm!", ex);
        } finally {
        }
    }

    /**
     *
     * @param args
     */
    public static void main(String[] args) {

        /* Setup CLParser */
        JPService.setApplicationName(ControlAmbientLightViaRemoteLib.class.getSimpleName().toLowerCase());
        JPService.parseAndExitOnError(args);

        try {
            new ControlAmbientLightViaRemoteLib().notifyAlarm();
        } catch (CouldNotPerformException | InterruptedException ex) {
            ExceptionPrinter.printHistory(ex, logger);
        }
    }
}
