package org.dc.bco.dal.example;

/*
 * #%L
 * DAL Example
 * %%
 * Copyright (C) 2014 - 2016 DivineCooperation
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

import org.dc.bco.dal.remote.unit.AmbientLightRemote;
import org.dc.jps.core.JPService;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.printer.ExceptionPrinter;
import org.dc.jul.exception.printer.LogLevel;
import java.awt.Color;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rsb.Scope;

/**
 *
 * @author * @author <a href="mailto:DivineThreepwood@gmail.com">Divine Threepwood</a>
 */
public class ControlAmbientLightViaRemoteLib {

    private static final Logger logger = LoggerFactory.getLogger(ControlAmbientLightViaRemoteLib.class);

    public void notifyAlarm() throws CouldNotPerformException, InterruptedException {

        final AmbientLightRemote testLight = new AmbientLightRemote();

        try {
            testLight.init(new Scope("/home/control/ambientlight/testunit_0/"));
            testLight.activate();

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
            testLight.shutdown();
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
        } catch(CouldNotPerformException | InterruptedException ex) {
            ExceptionPrinter.printHistory(ex, logger);
        }
    }
}
