package org.openbase.bco.dal.lib.layer.unit;

/*
 * #%L
 * BCO DAL Library
 * %%
 * Copyright (C) 2014 - 2016 openbase.org
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
import org.openbase.bco.registry.lib.util.UnitConfigProcessor;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.VerificationFailedException;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class UnitProcessor {

    public static boolean isHostUnit(final Unit unit) throws CouldNotPerformException {
        return UnitConfigProcessor.isHostUnit(unit.getConfig());
    }

    public static boolean isDalUnit(final Unit unit) throws CouldNotPerformException {
        return UnitConfigProcessor.isDalUnit(unit.getConfig());
    }

    public static boolean isBaseUnit(final Unit unit) throws CouldNotPerformException {
        return UnitConfigProcessor.isBaseUnit(unit.getConfig());
    }

    public static void verifyUnitType(final Unit unit) throws VerificationFailedException {
        try {
            UnitConfigProcessor.verifyUnitType(unit.getConfig(), unit.getType());
        } catch (NotAvailableException ex) {
            throw new VerificationFailedException("Could not verify unit type!", ex);
        }
    }

    public static void verifyUnitConfig(final Unit unit) throws VerificationFailedException {
        try {
            UnitConfigProcessor.verifyUnitConfig(unit.getConfig(), unit.getType());
        } catch (NotAvailableException ex) {
            throw new VerificationFailedException("Could not verify unit type!", ex);
        }
    }

    public static void verifyUnit(final Unit unit) throws VerificationFailedException {
        verifyUnitConfig(unit);
        verifyUnitType(unit);
    }
}
