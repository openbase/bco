package org.openbase.bco.manager.device.binding.openhab.service;

/*-
 * #%L
 * BCO Manager Device Binding OpenHAB
 * %%
 * Copyright (C) 2015 - 2018 openbase.org
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

import org.openbase.bco.dal.lib.layer.unit.Unit;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.LogLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServiceFactoryTools {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceFactoryTools.class);

    public static boolean detectAutoRepeat(final Unit unit) {
        try {
            return unit.generateVariablePool().getValue("COMPANY").equalsIgnoreCase("philips");
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory("Could not detect auto repeat of "+unit+"!", ex, LOGGER, LogLevel.WARN);
        }
        return false;
    }
}
