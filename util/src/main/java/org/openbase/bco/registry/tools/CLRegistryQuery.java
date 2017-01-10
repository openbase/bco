package org.openbase.bco.registry.tools;

/*
 * #%L
 * BCO Registry Utility
 * %%
 * Copyright (C) 2014 - 2017 openbase.org
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

import org.openbase.bco.registry.device.remote.CachedDeviceRegistryRemote;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitTemplateType;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class CLRegistryQuery {

    /**
     * This is a command line tool to query registry entries.
     * Currently this is just a prototype implementation.
     *
     * TODOs
     * - JPService should be used in a future release.
     * - Detail usage page should be generated.
     * - Implement more generic on protobuf types.
     *
     * @param args
     */
    public static void main(String[] args) {
        try {
            if ("type".equals(args[0])) {
                printUnitIdByType(UnitTemplateType.UnitTemplate.UnitType.valueOf(args[1]));
            }
        } catch (Exception ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Could not query!", ex), System.err);
        }
    }

    public static void printUnitIdByType(final UnitType unitType) throws InterruptedException, CouldNotPerformException {
        for (UnitConfig unitConfig : CachedDeviceRegistryRemote.getRegistry().getUnitConfigs()) {
            if (unitConfig.getType().equals(unitType)) {
                System.out.println(unitConfig.getId());
            }
        }
    }
}
